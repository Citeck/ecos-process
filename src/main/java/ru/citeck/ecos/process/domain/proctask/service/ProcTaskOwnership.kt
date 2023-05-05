package ru.citeck.ecos.process.domain.proctask.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.perms.EcosPermissionDelegateApi
import ru.citeck.ecos.webapp.api.perms.PermissionType

@Component
class ProcTaskOwnership(
    private val procTaskService: ProcTaskService,
    private val ecosPermissionDelegateApi: EcosPermissionDelegateApi,
    private val recordsService: RecordsService
) {

    companion object {
        const val GROUP_TASKS_REASSIGN_ALLOWED = "GROUP_WORKFLOW_TASKS_REASSIGN_ALLOWED"
    }

    fun performAction(data: TaskOwnershipChangeData) {
        with(data) {
            val task = procTaskService.getTaskById(taskId) ?: error("Task with id $taskId found")

            checkPermission(task)

            when (action) {
                TaskOwnershipAction.CLAIM -> procTaskService.claimTask(taskId, userId)
                TaskOwnershipAction.UNCLAIM -> procTaskService.unclaimTask(taskId)
                TaskOwnershipAction.CHANGE -> {
                    procTaskService.setAssignee(taskId, userId)

                    val currentUser = AuthContext.getCurrentUser()

                    if (task.documentRef.isNotEmpty() && userId.dontHaveReadPermsToRecord(task.documentRef)) {
                        ecosPermissionDelegateApi.delegate(task.documentRef, PermissionType.READ, currentUser, userId)
                    }
                }
            }
        }
    }

    private fun String.dontHaveReadPermsToRecord(record: EntityRef): Boolean {
        return AuthContext.runAs(this) {
            !recordsService.getAtt(
                record,
                "permissions._has.Read?bool!"
            ).asBoolean()
        }
    }

    private fun checkPermission(task: ProcTaskDto) {
        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return
        }

        if (task.currentUserIsTaskActor()) {
            return
        }

        if (AuthContext.getCurrentAuthorities().contains(GROUP_TASKS_REASSIGN_ALLOWED)) {
            return
        }

        error("Change task ownership not allowed.")
    }
}

data class TaskOwnershipChangeData(
    val action: TaskOwnershipAction,
    val taskId: String,
    val userId: String
)

enum class TaskOwnershipAction(val action: String) {
    CLAIM("claim"),
    UNCLAIM("unclaim"),
    CHANGE("change")
}
