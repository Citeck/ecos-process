package ru.citeck.ecos.process.domain.bpmn.engine.camunda.patch

import mu.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.splitToUserGroupCandidates
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import java.util.concurrent.Callable

@Component
@EcosPatch("bpmn-task-single-candidate-user-to-assignee", "2023-05-02T00:00:00Z")
class BpmnTaskSingleCandidateUserToAssigneePatch(
    private val camundaTaskService: TaskService
) : Callable<Any> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun call(): Any {

        val tasksWithCandidateUsers = camundaTaskService.createTaskQuery()
            .withCandidateUsers()
            .withoutCandidateGroups()
            .taskUnassigned()
            .initializeFormKeys()
            .list()

        log.info { "Found ${tasksWithCandidateUsers.size} for BpmnTaskSingleCandidateUserToAssigneePatch patch" }

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

        return "Patched ${filtered.size} tasks"
    }
}
