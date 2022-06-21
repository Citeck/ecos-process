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
        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        log.debug {
            "getTasksByProcessForCurrentUser: processId=$processId " +
                "user=$currentUser userAuthorities=$currentAuthorities"
        }

        return getTasksByProcess(processId).filter {
            isTaskActor(it, currentUser, currentAuthorities)
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
        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        log.debug {
            "getTasksByDocumentForCurrentUser: document=$document " +
                "user=$currentUser userAuthorities=$currentAuthorities"
        }

        return getTasksByDocument(document).filter {
            isTaskActor(it, currentUser, currentAuthorities)
        }
    }

    override fun getTaskById(taskId: String): ProcTaskDto? {
        // TODO: remove
        val variables = camundaTaskService.getVariables(taskId)
        val variablesLocal = camundaTaskService.getVariablesLocal(taskId)

        val identityLinksForTask = camundaTaskService.getIdentityLinksForTask(taskId)

        val task = camundaTaskService.createTaskQuery().taskId(taskId).initializeFormKeys().singleResult()

        val formKey = task.formKey
        val camundaFormRef = task.camundaFormRef

        return task.toProcTask()
    }

    override fun submitTaskForm(taskId: String, variables: Map<String, Any?>) {
        camundaTaskFormService.submitTaskForm(taskId, variables)
    }
}
