package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.converter.splitToUserGroupCandidates
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import kotlin.system.measureTimeMillis

@Service
class ProcTaskServiceImpl(
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService,
    private val cacheableTaskConverter: CacheableTaskConverter
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
            it.currentUserIsTaskActor()
        }
    }

    override fun getTasksByDocument(document: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(BPMN_DOCUMENT_REF, document)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByDocumentForCurrentUser: document=$document"
        }

        return getTasksByDocument(document).filter {
            it.currentUserIsTaskActor()
        }
    }

    override fun getTaskById(taskId: String): ProcTaskDto? {
        val result: ProcTaskDto?
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .initializeFormKeys()
                .singleResult()
                ?.toProcTask()
        }

        log.trace { "Get Camunda Task by id: time=$time ms" }

        return result
    }

    override fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?> {
        val result: List<ProcTaskDto?>
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskIdIn(*taskIds.toTypedArray())
                .initializeFormKeys()
                .list()
                .map {
                    val procTask: ProcTaskDto?
                    val time = measureTimeMillis {
                        procTask = it.toProcTask()
                    }

                    log.trace { "Task to procTask: $time ms" }

                    procTask
                }
        }

        log.debug { "Get Camunda Tasks by ids: $time ms" }

        return result
    }

    override fun completeTask(taskId: String, outcome: Outcome, variables: Map<String, Any?>) {
        val currentUser = AuthContext.getCurrentUser()

        camundaTaskService.setVariableLocal(taskId, BPMN_TASK_COMPLETED_BY, currentUser)

        val completionVariables = variables.toMutableMap()
        completionVariables[outcome.outcomeId()] = outcome.value
        completionVariables[outcome.nameId()] = outcome.name.toString()
        completionVariables[BPMN_LAST_TASK_COMPLETOR] = currentUser

        log.debug { "Complete task: taskId=$taskId, outcome=$outcome, variables=$completionVariables" }

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
        camundaTaskFormService.submitTaskForm(taskId, completionVariables)
    }

    override fun getVariables(taskId: String): Map<String, Any?> {
        return camundaTaskService.getVariables(taskId)
    }

    override fun claimTask(taskId: String, userId: String) {
        camundaTaskService.claim(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun unclaimTask(taskId: String) {
        camundaTaskService.setAssignee(taskId, null)
        returnCandidatesFromOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun setAssignee(taskId: String, userId: String) {
        camundaTaskService.setAssignee(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    private fun moveCandidatesToOriginalAtts(taskId: String) {
        camundaTaskService.getIdentityLinksForTask(taskId)?.run {
            val (candidateUsers, candidateGroups) = splitToUserGroupCandidates()

            if (candidateUsers.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_USER_ORIGINAL,
                    candidateUsers.toList()
                )
            }

            if (candidateGroups.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_GROUP_ORIGINAL,
                    candidateGroups.toList()
                )
            }

            log.debug {
                "Move candidates to original attributes: taskId=$taskId, " +
                    "users=$candidateUsers, groups=$candidateGroups"
            }

            candidateUsers.forEach { camundaTaskService.deleteCandidateUser(taskId, it) }
            candidateGroups.forEach { camundaTaskService.deleteCandidateGroup(taskId, it) }
        }
    }

    private fun returnCandidatesFromOriginalAtts(taskId: String) {
        @Suppress("UNCHECKED_CAST")
        val originalCandidateUsers = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_USER_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateUsers.forEach {
            camundaTaskService.addCandidateUser(taskId, it)
        }

        @Suppress("UNCHECKED_CAST")
        val originalCandidateGroups = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateGroups.forEach {
            camundaTaskService.addCandidateGroup(taskId, it)
        }

        log.debug {
            "Return candidates from original attributes: taskId=$taskId, " +
                "users=$originalCandidateUsers, groups=$originalCandidateGroups"
        }

        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_USER_ORIGINAL)
        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL)
    }
}
