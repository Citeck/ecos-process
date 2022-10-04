package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

@Service
class ProcTaskServiceImpl(
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService
) : ProcTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getTasksByProcess(processId: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processInstanceId(processId)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByProcessForCurrentUser: processId=$processId "
        }

        return getTasksByProcess(processId).filter {
            currentUserIsTaskActor(it)
        }
    }

    override fun getTasksByDocument(document: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(VAR_DOCUMENT_REF, document)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByDocumentForCurrentUser: document=$document"
        }

        return getTasksByDocument(document).filter {
            currentUserIsTaskActor(it)
        }
    }

    override fun getTaskById(taskId: String): ProcTaskDto? {
        return camundaTaskService.createTaskQuery()
            .taskId(taskId)
            .initializeFormKeys()
            .singleResult()
            ?.toProcTask()
    }

    override fun completeTask(taskId: String, variables: Map<String, Any?>) {
        val currentUser = AuthContext.getCurrentUser()
        camundaTaskService.setVariableLocal(taskId, TASK_COMPLETED_BY, currentUser)

        camundaTaskFormService.submitTaskForm(taskId, variables)
    }
}
