package ru.citeck.ecos.process.domain.proctask.candidatessync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_USER_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val ATT_UPDATE_PERMISSIONS = "__updatePermissions"

/**
 * Re-resolves candidateUsers/candidateGroups of active tasks against current role membership.
 *
 * Role assignment happens only once, at task creation (see RecipientsFromRolesUserTaskAssignListener),
 * so a role's membership change made after task creation is never reflected in already-running tasks.
 * This service re-runs the same resolution (TaskDefinitionUtils role names + RoleService.getAssignees)
 * and reconciles it with the task's current identity links / assignee.
 */
@Service
class ProcTaskCandidatesSyncService(
    private val procTaskService: ProcTaskService,
    private val taskDefinitionUtils: TaskDefinitionUtils,
    private val roleService: RoleService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val camundaTaskService: TaskService,
    private val cacheableTaskConverter: CacheableTaskConverter,
    private val recordsService: RecordsService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun syncCandidates(documentRef: EntityRef): ProcTaskCandidatesSyncResult {
        val updatedTaskIds = mutableListOf<String>()
        val unassignedTaskIds = mutableListOf<String>()

        AuthContext.runAsSystem {
            procTaskService.getTasksByDocument(documentRef.toString())
                .filter { !it.historic }
                .forEach { task ->
                    val syncedTask = syncTaskCandidates(documentRef, task)
                    if (syncedTask.changed) {
                        updatedTaskIds.add(task.id)
                    }
                    if (syncedTask.unassigned) {
                        unassignedTaskIds.add(task.id)
                    }
                }
        }

        log.info {
            "Synced candidates for document $documentRef. " +
                "Updated tasks: $updatedTaskIds, unassigned tasks: $unassignedTaskIds"
        }

        if (updatedTaskIds.isNotEmpty()) {
            AuthContext.runAsSystem {
                log.info { "Requesting permissions recalculation for document $documentRef" }
                try {
                    recordsService.mutate(documentRef, ObjectData.create().set(ATT_UPDATE_PERMISSIONS, true))
                    log.info { "Permissions recalculation request for document $documentRef completed" }
                } catch (e: Exception) {
                    log.warn(e) { "Failed to request permissions recalculation for document $documentRef" }
                }
            }
        }

        return ProcTaskCandidatesSyncResult(updatedTaskIds, unassignedTaskIds)
    }

    private fun syncTaskCandidates(documentRef: EntityRef, task: ProcTaskDto): TaskSyncOutcome {
        val processDefinitionId = task.processDefinitionId ?: return TaskSyncOutcome.NONE
        val taskDefinitionKey = task.definitionKey ?: return TaskSyncOutcome.NONE

        val roles = taskDefinitionUtils.getTaskRoleNames(processDefinitionId, taskDefinitionKey)
        if (roles.isEmpty()) {
            // manual recipients / multi-instance auto mode task - not driven by roles
            return TaskSyncOutcome.NONE
        }

        val resolvedNames = roles.flatMap { role ->
            val assignees = roleService.getAssignees(documentRef, role)
            log.info { "Task ${task.id}, document $documentRef, role '$role' resolved to assignees: $assignees" }
            assignees
        }.distinct()
        val normalizedNames = if (resolvedNames.isEmpty()) {
            emptySet()
        } else {
            authoritiesApi.getAuthorityNames(resolvedNames).toSet()
        }
        log.info { "Task ${task.id}, document $documentRef, resolved names: $resolvedNames, normalized: $normalizedNames" }

        val targetUsers = normalizedNames.filterNot { it.startsWith(AuthGroup.PREFIX) }.toSet()
        val targetGroups = normalizedNames.filter { it.startsWith(AuthGroup.PREFIX) }.toSet()

        return if (EntityRef.isNotEmpty(task.assignee)) {
            syncAssignedTask(task, targetUsers, targetGroups)
        } else {
            syncUnassignedTask(task, targetUsers, targetGroups)
        }
    }

    private fun syncAssignedTask(
        task: ProcTaskDto,
        targetUsers: Set<String>,
        targetGroups: Set<String>
    ): TaskSyncOutcome {
        val assigneeName = authoritiesApi.getAuthorityName(task.assignee)
        val currentUsers = task.candidateUsersOriginal.toSet()
        val currentGroups = task.candidateGroupsOriginal.toSet()

        if (currentUsers.isEmpty() && currentGroups.isEmpty()) {
            // No stashed originals means the assignee was never really claimed from a candidate
            // set - it's the auto-assign-when-single-candidate shortcut (see fillTaskRecipients),
            // which sets the assignee directly without ever touching identity links. There is
            // nothing to restore via unclaimTask here. If the role still resolves to exactly this
            // one person, nothing changed; otherwise drop the assignee and populate the live
            // identity links with the fresh target set directly.
            if (targetUsers == setOf(assigneeName) && targetGroups.isEmpty()) {
                return TaskSyncOutcome.NONE
            }

            log.info {
                "Task ${task.id}: assignee $assigneeName was auto-assigned as the sole candidate " +
                    "(no stashed originals) and the role no longer resolves to just them - " +
                    "dropping assignee and switching to targetUsers=$targetUsers, targetGroups=$targetGroups"
            }
            camundaTaskService.setAssignee(task.id, null)
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
            return syncUnassignedTask(task, targetUsers, targetGroups).copy(changed = true, unassigned = true)
        }

        var changed = false

        if (currentUsers != targetUsers || currentGroups != targetGroups) {
            camundaTaskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_USER_ORIGINAL, targetUsers.toList())
            camundaTaskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL, targetGroups.toList())
            changed = true
        }

        val assigneeStillEligible = assigneeName in targetUsers ||
            (targetGroups.isNotEmpty() && authoritiesApi.getUserAuthorities(assigneeName).any { it in targetGroups })
        val unassigned = !assigneeStillEligible
        if (unassigned) {
            // Assignee is no longer a member of the role - directly, or via a candidate group they
            // claimed through - return to candidates (already fresh - see above) and clear the
            // assignment via the existing unclaim path.
            procTaskService.unclaimTask(task.id)
            changed = true
        }

        if (changed) {
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
        }

        return TaskSyncOutcome(changed, unassigned)
    }

    private fun syncUnassignedTask(
        task: ProcTaskDto,
        targetUsers: Set<String>,
        targetGroups: Set<String>
    ): TaskSyncOutcome {
        val currentUsers = task.candidateUsers.map { authoritiesApi.getAuthorityName(it) }.toSet()
        val currentGroups = task.candidateGroups.map { authoritiesApi.getAuthorityName(it) }.toSet()

        log.info {
            "Task ${task.id} unassigned sync: currentUsers=$currentUsers, targetUsers=$targetUsers, " +
                "currentGroups=$currentGroups, targetGroups=$targetGroups"
        }

        var changed = false

        (targetUsers - currentUsers).forEach {
            log.info { "Task ${task.id}: addCandidateUser($it)" }
            camundaTaskService.addCandidateUser(task.id, it)
            changed = true
        }
        (currentUsers - targetUsers).forEach {
            log.info { "Task ${task.id}: deleteCandidateUser($it)" }
            camundaTaskService.deleteCandidateUser(task.id, it)
            changed = true
        }
        (targetGroups - currentGroups).forEach {
            log.info { "Task ${task.id}: addCandidateGroup($it)" }
            camundaTaskService.addCandidateGroup(task.id, it)
            changed = true
        }
        (currentGroups - targetGroups).forEach {
            log.info { "Task ${task.id}: deleteCandidateGroup($it)" }
            camundaTaskService.deleteCandidateGroup(task.id, it)
            changed = true
        }

        if (changed) {
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
        }

        return TaskSyncOutcome(changed, unassigned = false)
    }

    private data class TaskSyncOutcome(val changed: Boolean, val unassigned: Boolean) {
        companion object {
            val NONE = TaskSyncOutcome(changed = false, unassigned = false)
        }
    }
}

data class ProcTaskCandidatesSyncResult(
    val updatedTaskIds: List<String>,
    val unassignedTaskIds: List<String>
)
