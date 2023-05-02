package ru.citeck.ecos.process.domain.proctask.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext

@Component
class ProcTaskOwnership(
    private val procTaskService: ProcTaskService
) {

    companion object {
        const val GROUP_TASKS_REASSIGN_ALLOWED = "GROUP_WORKFLOW_TASKS_REASSIGN_ALLOWED"
    }

    fun performAction(data: TaskOwnershipChangeData) {

        checkPermission(data)

        when (data.action) {
            TaskOwnershipAction.CLAIM -> procTaskService.claimTask(data.taskId, data.userId)
            TaskOwnershipAction.UNCLAIM -> procTaskService.unclaimTask(data.taskId)
            TaskOwnershipAction.CHANGE -> procTaskService.setAssignee(data.taskId, data.userId)
        }
    }

    private fun checkPermission(data: TaskOwnershipChangeData) {
        val task = procTaskService.getTaskById(data.taskId) ?: error("Task with id ${data.taskId} found")

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
