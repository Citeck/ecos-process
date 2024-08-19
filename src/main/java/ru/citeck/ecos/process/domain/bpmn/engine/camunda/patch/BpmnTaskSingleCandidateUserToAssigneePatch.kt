package ru.citeck.ecos.process.domain.bpmn.engine.camunda.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.splitToUserGroupCandidates
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import java.util.concurrent.Callable

private const val BATCH_SIZE = 100

@Component
@EcosPatch("bpmn-task-single-candidate-user-to-assignee_5", "2023-05-02T00:00:00Z")
class BpmnTaskSingleCandidateUserToAssigneePatch(
    private val camundaTaskService: TaskService
) : Callable<Any> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun call(): Any {

        var firstResult = 0
        var totalFoundFilteredTasks = 0
        var tasksWithCandidateUsers = findUnassignedTasksWithCandidateUsers(firstResult)

        while (tasksWithCandidateUsers.isNotEmpty()) {

            log.info {
                "Found ${tasksWithCandidateUsers.size} for BpmnTaskSingleCandidateUserToAssigneePatch patch. " +
                    "FirstResult size: $firstResult "
            }

            val filtered = tasksWithCandidateUsers
                .filter {
                    val links = camundaTaskService.getIdentityLinksForTask(it.id)
                    val (candidateUsers, candidateGroups) = links.splitToUserGroupCandidates()
                    it.assignee == null && candidateUsers.size == 1 && candidateGroups.isEmpty()
                }

            log.info { "Filtered ${filtered.size} for BpmnTaskSingleCandidateUserToAssigneePatch patch" }

            filtered.forEach {
                log.info { "Patch SingleCandidateUser task ${it.id}" }

                val assigneeUser = camundaTaskService.getIdentityLinksForTask(it.id).first().userId
                it.assignee = assigneeUser
                camundaTaskService.deleteCandidateUser(it.id, assigneeUser)
            }

            totalFoundFilteredTasks += filtered.size
            firstResult += tasksWithCandidateUsers.size

            tasksWithCandidateUsers = findUnassignedTasksWithCandidateUsers(firstResult)
        }

        val msg = "Patched $totalFoundFilteredTasks tasks of found $firstResult tasks"

        log.info { msg }

        return msg
    }

    private fun findUnassignedTasksWithCandidateUsers(skip: Int): List<Task> {
        return camundaTaskService.createTaskQuery()
            .withCandidateUsers()
            .withoutCandidateGroups()
            .taskUnassigned()
            .initializeFormKeys()
            .listPage(skip, BATCH_SIZE)
    }
}
