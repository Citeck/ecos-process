package ru.citeck.ecos.process.domain.proctask.service

import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
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

    fun getTaskById(taskId: String): ProcTaskDto? {
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
