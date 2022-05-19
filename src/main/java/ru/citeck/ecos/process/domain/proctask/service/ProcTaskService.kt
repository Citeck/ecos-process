package ru.citeck.ecos.process.domain.proctask.service

import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

@Service
class ProcTaskService(
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService
) {

    fun getTasksByProcess(processId: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processInstanceId(processId)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    fun getTasksByDocument(document: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(VAR_DOCUMENT, document)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto> {
        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        return getTasksByDocument(document).filter {
            isTaskActor(it, currentUser, currentAuthorities)
        }
    }

    private fun isTaskActor(task: ProcTaskDto, currentUser: String, currentUserAuthorities: List<String>): Boolean {
        val assigneeName = task.assignee.id
        val candidateUsers = task.candidateUsers.map { it.id }
        val candidateGroup = task.candidateGroups.map { it.id }

        if (assigneeName == currentUser) return true
        if (candidateUsers.contains(currentUser)) return true
        if (candidateGroup.any { it in currentUserAuthorities }) return true

        return false
    }

    fun getTaskById(taskId: String): ProcTaskDto? {
        // TODO: remove
        val variables = camundaTaskService.getVariables(taskId)
        val variablesLocal = camundaTaskService.getVariablesLocal(taskId)

        val identityLinksForTask = camundaTaskService.getIdentityLinksForTask(taskId)

        val task = camundaTaskService.createTaskQuery().taskId(taskId).initializeFormKeys().singleResult()

        val formKey = task.formKey
        val camundaFormRef = task.camundaFormRef

        return task.toProcTask()
    }

    fun submitTaskForm(taskId: String, variables: Map<String, Any>) {
        camundaTaskFormService.submitTaskForm(taskId, variables)
    }
}
