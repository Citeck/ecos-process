package ru.citeck.ecos.process.domain.proctask.candidatessync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.context.ProcessEngineContext
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_USER_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_ROLE_RECIPIENTS_SNAPSHOT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.getSingleUserAssignee
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
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
 * Manual recipients and multi-instance auto tasks require different synchronization algorithms and are skipped.
 */
@Service
class ProcTaskCandidatesSyncService(
    private val procTaskService: ProcTaskService,
    private val taskDefinitionUtils: TaskDefinitionUtils,
    private val camundaRoleService: CamundaRoleService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val camundaTaskService: TaskService,
    private val cacheableTaskConverter: CacheableTaskConverter,
    private val camundaCommandExecutor: CommandExecutor,
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
            val taskIds = getActiveTaskIds(documentRef)

            taskIds.forEach { taskId ->
                val syncedTask = try {
                    syncTaskInTransaction(documentRef, taskId, resolvedRolesCache)
                } catch (e: Exception) {
                    log.error(e) { "Failed to sync candidates of task $taskId, document $documentRef" }
                    cacheableTaskConverter.removeFromActualTaskCache(taskId)
                    failedTaskIds.add(taskId)
                    return@forEach
                }

                // Evict after commit so another reader cannot retain a DTO loaded before commit.
                cacheableTaskConverter.removeFromActualTaskCache(taskId)

                if (syncedTask.changed) {
                    updatedTaskIds.add(taskId)
                }

                if (syncedTask.unassigned) {
                    unassignedTaskIds.add(taskId)
                }
            }
        }

        log.debug {
            "Synced candidates for document $documentRef. " +
                "Updated tasks: $updatedTaskIds, unassigned tasks: $unassignedTaskIds, failed tasks: $failedTaskIds"
        }

        return ProcTaskCandidatesSyncResult(updatedTaskIds, unassignedTaskIds, failedTaskIds)
    }

    private fun getActiveTaskIds(documentRef: EntityRef): List<String> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(BPMN_DOCUMENT_REF, documentRef.toString())
            .list()
            .map { it.id }
    }

    private fun syncTaskInTransaction(
        documentRef: EntityRef,
        taskId: String,
        resolvedRolesCache: MutableMap<String, Set<String>>
    ): TaskSyncOutcome {
        return taskTxnTemplate.execute {
            syncTaskInNewEngineContext(documentRef, taskId, resolvedRolesCache)
        } ?: TaskSyncOutcome.NONE
    }

    private fun syncTaskInNewEngineContext(
        documentRef: EntityRef,
        taskId: String,
        resolvedRolesCache: MutableMap<String, Set<String>>
    ): TaskSyncOutcome {
        // A caller may already be running a Camunda command. The new transaction must also get
        // its own entity cache, while the reads and writes of this task share that new context.
        return ProcessEngineContext.withNewProcessEngineContext {
            camundaCommandExecutor.execute(
                Command {
                    syncCurrentTaskCandidates(documentRef, taskId, resolvedRolesCache)
                }
            )
        }
    }

    private fun syncCurrentTaskCandidates(
        documentRef: EntityRef,
        taskId: String,
        resolvedRolesCache: MutableMap<String, Set<String>>
    ): TaskSyncOutcome {
        val currentTask = camundaTaskService.createTaskQuery()
            .taskId(taskId)
            .initializeFormKeys()
            .singleResult() ?: return TaskSyncOutcome.NONE

        val task = cacheableTaskConverter.convertTaskUncached(currentTask)

        return syncTaskCandidates(documentRef, task, resolvedRolesCache)
    }

    private fun syncTaskCandidates(
        documentRef: EntityRef,
        task: ProcTaskDto,
        resolvedRolesCache: MutableMap<String, Set<String>>
    ): TaskSyncOutcome {
        val processDefinitionId = task.processDefinitionId ?: return TaskSyncOutcome.NONE
        val taskDefinitionKey = task.definitionKey ?: return TaskSyncOutcome.NONE

        if (taskDefinitionUtils.isManualRecipientsMode(processDefinitionId, taskDefinitionKey) ||
            taskDefinitionUtils.isMultiInstanceAutoMode(processDefinitionId, taskDefinitionKey)
        ) {
            return TaskSyncOutcome.NONE
        }

        val roles = taskDefinitionUtils.getTaskRoleNames(processDefinitionId, taskDefinitionKey)
        if (roles.isEmpty()) {
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
            val recipients = current - diff.removed + diff.target
            updateTaskRecipients(task.id, current, recipients)
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
            returnTaskToRecipients(task, newOriginals)
            changed = true
        }

        return TaskSyncOutcome(changed, unassigned)
    }

    private fun syncUnassignedTask(task: ProcTaskDto, diff: RoleRecipientsDiff): TaskSyncOutcome {
        val current = getCurrentCandidates(task)
        val recipients = current - diff.removed + diff.added

        if (recipients == current) {
            return TaskSyncOutcome.NONE
        }

        updateTaskRecipients(task.id, current, recipients)

        return TaskSyncOutcome(changed = true, unassigned = false)
    }

    /**
     * Apply the same single-user assignment rule as at task creation. Compute the final recipients
     * before writing links: an added candidate may still be pending in this command context.
     */
    private fun updateTaskRecipients(taskId: String, current: Set<String>, recipients: Set<String>) {
        val assignee = getSingleUserAssignee(recipients)

        if (assignee == null) {
            updateCandidates(taskId, add = recipients - current, remove = current - recipients)
        } else {
            updateCandidates(taskId, add = emptySet(), remove = current)
            camundaTaskService.setAssignee(taskId, assignee)
        }
    }

    private fun returnTaskToRecipients(task: ProcTaskDto, originals: Set<String>) {
        val current = getCurrentCandidates(task)
        val recipients = current + originals
        val assignee = getSingleUserAssignee(recipients)

        if (assignee == null) {
            procTaskService.unclaimTask(task.id)
            return
        }

        // Unclaim without creating a temporary candidate link for the sole user. The final
        // assignment is applied directly, and the original-candidate variables are still cleared.
        camundaTaskService.removeVariablesLocal(
            task.id,
            listOf(BPMN_TASK_CANDIDATES_USER_ORIGINAL, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL)
        )
        procTaskService.unclaimTask(task.id)
        updateTaskRecipients(task.id, current, recipients)
    }

    private fun updateCandidates(taskId: String, add: Set<String>, remove: Set<String>) {
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

        removeDeletedIdentityLinksFromTaskCache(taskId)
    }

    private fun removeDeletedIdentityLinksFromTaskCache(taskId: String) {
        val commandContext = Context.getCommandContext()
        val task = commandContext.taskManager.findTaskById(taskId)

        // Assignment listeners read this list directly. Keep their event data consistent with the
        // pending deletions without flushing the command context in the middle of the operation.
        task.identityLinks.removeIf { commandContext.dbEntityManager.isDeleted(it) }
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

    /**
     * Camunda's local-variable setter force-updates the task row. Saving the snapshot therefore
     * checks the revision read at the start of this command, even when the assignee is kept.
     */
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
