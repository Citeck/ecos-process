package ru.citeck.ecos.process.domain.proctask.candidatessync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_USER_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_ROLE_RECIPIENTS_SNAPSHOT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.getSingleUserAssignee
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.splitToUserGroupCandidates
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Re-resolves candidateUsers/candidateGroups of active tasks against current role membership.
 *
 * Role assignment happens only once, at task creation (see RecipientsFromRolesUserTaskAssignListener),
 * so a role's membership change made after task creation is never reflected in already-running tasks.
 * This service re-runs the same resolution (TaskDefinitionUtils role names + CamundaRoleService)
 * and applies only the difference between the previous role composition (the snapshot saved in the task,
 * see [BPMN_TASK_ROLE_RECIPIENTS_SNAPSHOT]) and the new one:
 * - members dropped from the role are removed from candidates, members added to the role are added;
 * - the assignee is unassigned only if they came from the role and dropped out of it. An assignee who didn't
 *   come from the role (assignee changed manually, delegate) is left as is;
 * - recipients added to the task not from the role are left as is;
 * - as at task creation, a task left with a single user candidate is assigned to them directly.
 *
 * Tasks in manual recipients / multi-instance auto mode are not driven by roles and are skipped.
 */
@Service
class ProcTaskCandidatesSyncService(
    private val procTaskService: ProcTaskService,
    private val taskDefinitionUtils: TaskDefinitionUtils,
    private val camundaRoleService: CamundaRoleService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val camundaTaskService: TaskService,
    private val cacheableTaskConverter: CacheableTaskConverter,
    transactionManager: PlatformTransactionManager
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val taskTxnTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * Each task is synced in its own transaction: a failure on one task rolls back only its own changes,
     * is logged, and doesn't stop the sync of the other tasks of the document.
     */
    fun syncCandidates(documentRef: EntityRef): ProcTaskCandidatesSyncResult {
        val updatedTaskIds = mutableListOf<String>()
        val unassignedTaskIds = mutableListOf<String>()
        val failedTaskIds = mutableListOf<String>()

        // Role composition of the document doesn't change within one sync call - resolve each roles set once
        val resolvedRolesCache = mutableMapOf<String, Set<String>>()

        AuthContext.runAsSystem {
            procTaskService.getTasksByDocument(documentRef.toString())
                .forEach { task ->
                    val syncedTask = try {
                        taskTxnTemplate.execute {
                            syncTaskCandidates(documentRef, task, resolvedRolesCache)
                        } ?: TaskSyncOutcome.NONE
                    } catch (e: Exception) {
                        log.error(e) { "Failed to sync candidates of task ${task.id}, document $documentRef" }
                        cacheableTaskConverter.removeFromActualTaskCache(task.id)
                        failedTaskIds.add(task.id)
                        return@forEach
                    }
                    if (syncedTask.changed) {
                        updatedTaskIds.add(task.id)
                    }
                    if (syncedTask.unassigned) {
                        unassignedTaskIds.add(task.id)
                    }
                }
        }

        log.debug {
            "Synced candidates for document $documentRef. " +
                "Updated tasks: $updatedTaskIds, unassigned tasks: $unassignedTaskIds, failed tasks: $failedTaskIds"
        }

        return ProcTaskCandidatesSyncResult(updatedTaskIds, unassignedTaskIds, failedTaskIds)
    }

    private fun syncTaskCandidates(
        documentRef: EntityRef,
        task: ProcTaskDto,
        resolvedRolesCache: MutableMap<String, Set<String>>
    ): TaskSyncOutcome {
        val processDefinitionId = task.processDefinitionId ?: return TaskSyncOutcome.NONE
        val taskDefinitionKey = task.definitionKey ?: return TaskSyncOutcome.NONE

        val roles = taskDefinitionUtils.getTaskRoleNames(processDefinitionId, taskDefinitionKey)
        if (roles.isEmpty()) {
            // not driven by roles: no roles or manual recipients / multi-instance auto mode task
            return TaskSyncOutcome.NONE
        }

        val rolesStr = roles.joinToString(BPMN_CAMUNDA_COLLECTION_SEPARATOR)
        val target = resolvedRolesCache.getOrPut(rolesStr) { resolveRoles(documentRef, rolesStr) }

        if (target.isEmpty()) {
            log.warn {
                "Task ${task.id}, document $documentRef: roles $roles resolved to nobody. " +
                    "Task recipients are left unchanged"
            }
            return TaskSyncOutcome.NONE
        }

        val snapshot = getRoleRecipientsSnapshot(task.id)
        // Tasks created before the snapshot was introduced: take the current recipients as the previous
        // role composition
        val previous = snapshot ?: getCurrentRecipients(task)

        if (previous == target) {
            if (snapshot == null) {
                saveRoleRecipientsSnapshot(task.id, target)
            }
            return TaskSyncOutcome.NONE
        }

        val diff = RoleRecipientsDiff(previous, target)
        log.debug { "Task ${task.id}: role composition changed $previous -> $target" }

        val outcome = if (EntityRef.isNotEmpty(task.assignee)) {
            syncAssignedTask(task, diff)
        } else {
            syncUnassignedTask(task, diff)
        }

        saveRoleRecipientsSnapshot(task.id, target)

        return outcome
    }

    /**
     * Same resolution as at task creation: roles.getUserNames + roles.getGroupNames expression
     * (see CamundaUserTaskConverter), normalized by the assign listener (see UserTaskListenerUtils).
     */
    private fun resolveRoles(documentRef: EntityRef, rolesStr: String): Set<String> {
        val document = documentRef.toString()
        val resolvedNames = (
            camundaRoleService.getUserNames(document, rolesStr) +
                camundaRoleService.getGroupNames(document, rolesStr)
            ).distinct()
        val target = if (resolvedNames.isEmpty()) {
            emptySet()
        } else {
            authoritiesApi.getAuthorityNames(resolvedNames).toSet()
        }
        log.debug { "Document $documentRef, roles '$rolesStr' resolved to: $resolvedNames, normalized: $target" }
        return target
    }

    private fun syncAssignedTask(task: ProcTaskDto, diff: RoleRecipientsDiff): TaskSyncOutcome {
        val assigneeName = authoritiesApi.getAuthorityName(task.assignee)
        val originals = (task.candidateUsersOriginal + task.candidateGroupsOriginal).toSet()

        if (originals.isEmpty() && diff.previous == setOf(assigneeName)) {
            // The assignee was never really claimed from a candidate set - it's the
            // auto-assign-when-single-candidate shortcut (see fillTaskRecipients), which sets the assignee
            // directly without ever touching identity links. The role no longer resolves to just them -
            // drop the assignee and populate the live identity links with the new role composition.
            log.debug {
                "Task ${task.id}: assignee $assigneeName was auto-assigned as the sole candidate " +
                    "and the role no longer resolves to just them - dropping assignee, candidates: ${diff.target}"
            }
            camundaTaskService.setAssignee(task.id, null)
            val current = getCurrentCandidates(task)
            updateCandidates(task.id, add = diff.target - current, remove = current intersect diff.removed)
            assignSingleUserCandidate(task.id)
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
            return TaskSyncOutcome(changed = true, unassigned = true)
        }

        var changed = false

        // Candidates to return to on unclaim. Empty originals means the assignee was set manually on
        // a task without candidates - take the whole new role composition then.
        val newOriginals = if (originals.isEmpty()) {
            diff.target
        } else {
            originals - diff.removed + diff.added
        }
        if (newOriginals != originals) {
            camundaTaskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_USER_ORIGINAL, newOriginals.users())
            camundaTaskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL, newOriginals.groups())
            changed = true
        }

        // Unassign only an assignee who came from the role and dropped out of it - directly, or via
        // a candidate group they claimed through. Assignee set manually or a delegate is left as is.
        val cameFromRole = isMemberOf(assigneeName, diff.previous)
        val unassigned = cameFromRole && !isMemberOf(assigneeName, diff.target)
        if (unassigned) {
            log.debug { "Task ${task.id}: assignee $assigneeName dropped out of the role - unassigning" }
            procTaskService.unclaimTask(task.id)
            assignSingleUserCandidate(task.id)
            changed = true
        }

        if (changed) {
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
        }

        return TaskSyncOutcome(changed, unassigned)
    }

    private fun syncUnassignedTask(task: ProcTaskDto, diff: RoleRecipientsDiff): TaskSyncOutcome {
        val current = getCurrentCandidates(task)
        val changed = updateCandidates(
            task.id,
            add = diff.added - current,
            remove = diff.removed intersect current
        )

        if (changed) {
            assignSingleUserCandidate(task.id)
            cacheableTaskConverter.removeFromActualTaskCache(task.id)
        }

        return TaskSyncOutcome(changed, unassigned = false)
    }

    /**
     * Same rule as at task creation (see fillTaskRecipients): a task with a single user candidate
     * is assigned to them directly instead of being shared.
     */
    private fun assignSingleUserCandidate(taskId: String) {
        val (candidateUsers, candidateGroups) = camundaTaskService.getIdentityLinksForTask(taskId)
            .splitToUserGroupCandidates()
        val assignee = getSingleUserAssignee(candidateUsers + candidateGroups) ?: return

        log.debug { "Task $taskId: $assignee is the only candidate - assigning the task to them" }
        camundaTaskService.deleteCandidateUser(taskId, assignee)
        camundaTaskService.setAssignee(taskId, assignee)
    }

    private fun updateCandidates(taskId: String, add: Set<String>, remove: Set<String>): Boolean {
        log.debug { "Task $taskId: add candidates $add, remove candidates $remove" }

        add.forEach {
            if (it.isGroup()) {
                camundaTaskService.addCandidateGroup(taskId, it)
            } else {
                camundaTaskService.addCandidateUser(taskId, it)
            }
        }
        remove.forEach {
            if (it.isGroup()) {
                camundaTaskService.deleteCandidateGroup(taskId, it)
            } else {
                camundaTaskService.deleteCandidateUser(taskId, it)
            }
        }

        return add.isNotEmpty() || remove.isNotEmpty()
    }

    private fun isMemberOf(userName: String, recipients: Set<String>): Boolean {
        if (userName in recipients) {
            return true
        }
        val groups = recipients.filter { it.isGroup() }
        return groups.isNotEmpty() && authoritiesApi.getUserAuthorities(userName).any { it in groups }
    }

    private fun getCurrentCandidates(task: ProcTaskDto): Set<String> {
        return (task.candidateUsers + task.candidateGroups).map { authoritiesApi.getAuthorityName(it) }.toSet()
    }

    private fun getCurrentRecipients(task: ProcTaskDto): Set<String> {
        if (EntityRef.isEmpty(task.assignee)) {
            return getCurrentCandidates(task)
        }
        val originals = (task.candidateUsersOriginal + task.candidateGroupsOriginal).toSet()
        return originals.ifEmpty { setOf(authoritiesApi.getAuthorityName(task.assignee)) }
    }

    private fun getRoleRecipientsSnapshot(taskId: String): Set<String>? {
        val snapshot = camundaTaskService.getVariableLocal(taskId, BPMN_TASK_ROLE_RECIPIENTS_SNAPSHOT) ?: return null
        @Suppress("UNCHECKED_CAST")
        val names = snapshot as? List<String> ?: return null
        if (names.isEmpty()) {
            return emptySet()
        }
        return authoritiesApi.getAuthorityNames(names).toSet()
    }

    private fun saveRoleRecipientsSnapshot(taskId: String, recipients: Set<String>) {
        camundaTaskService.setVariableLocal(taskId, BPMN_TASK_ROLE_RECIPIENTS_SNAPSHOT, ArrayList(recipients))
    }

    private fun String.isGroup(): Boolean = startsWith(AuthGroup.PREFIX)

    private fun Set<String>.users(): List<String> = filterNot { it.isGroup() }

    private fun Set<String>.groups(): List<String> = filter { it.isGroup() }

    private class RoleRecipientsDiff(val previous: Set<String>, val target: Set<String>) {
        val added: Set<String> = target - previous
        val removed: Set<String> = previous - target
    }

    private data class TaskSyncOutcome(val changed: Boolean, val unassigned: Boolean) {
        companion object {
            val NONE = TaskSyncOutcome(changed = false, unassigned = false)
        }
    }
}

data class ProcTaskCandidatesSyncResult(
    val updatedTaskIds: List<String>,
    val unassignedTaskIds: List<String>,
    val failedTaskIds: List<String> = emptyList()
)
