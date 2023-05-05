package ru.citeck.ecos.process.domain.proctask.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.perms.EcosPermissionDelegateApi
import ru.citeck.ecos.webapp.api.perms.PermissionType

const val TASK_OWNERSHIP_REASSIGN_ALLOWED_GROUP = "GROUP_WORKFLOW_TASKS_REASSIGN_ALLOWED"

@Component
class ProcTaskOwnership(
    private val procTaskService: ProcTaskService,
    private val ecosPermissionDelegateApi: EcosPermissionDelegateApi,
    private val recordsService: RecordsService
) {

    companion object {
        private const val PERSON_SOURCE_ID = "person"
        private const val PERSON_AUTHORITIES_LIST_ATT = "authorities.list[]"
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

                    if (task.documentRef.isNotEmpty() && dontHaveReadPermsToRecord(userId, task.documentRef)) {
                        ecosPermissionDelegateApi.delegate(task.documentRef, PermissionType.READ, currentUser, userId)
                    }
                }
            }
        }
    }

    private fun dontHaveReadPermsToRecord(userId: String, record: EntityRef): Boolean {
        val userAuthorities = recordsService.getAtt(
            EntityRef.create(AppName.EMODEL, PERSON_SOURCE_ID, userId), PERSON_AUTHORITIES_LIST_ATT
        ).asStrList()

        return AuthContext.runAs(userId, userAuthorities) {
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

        if (AuthContext.getCurrentAuthorities().contains(TASK_OWNERSHIP_REASSIGN_ALLOWED_GROUP)) {
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
