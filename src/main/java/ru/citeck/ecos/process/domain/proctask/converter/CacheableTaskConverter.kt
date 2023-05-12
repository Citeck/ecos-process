package ru.citeck.ecos.process.domain.proctask.converter

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.task.Task
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.proctask.config.PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASKS_DTO_CONVERTER_CACHE_KEY
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName

/**
 * @author Roman Makarskiy
 */
@Component
class CacheableTaskConverter(
    private val camundaTaskService: TaskService,
    private val camundaHistoryService: HistoryService,
    private val authorityService: EcosAuthoritiesApi
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @CacheEvict(cacheNames = [PROC_TASKS_DTO_CONVERTER_CACHE_KEY], key = "#taskId")
    fun removeFromActualTaskCache(taskId: String) {
        log.debug { "CacheableTaskConverter remove from cache taskId=$taskId" }
    }

    @Cacheable(cacheNames = [PROC_TASKS_DTO_CONVERTER_CACHE_KEY], key = "#task.id")
    fun convertTask(task: Task): ProcTaskDto {
        with(task) {
            val links = camundaTaskService.getIdentityLinksForTask(id)
            val variables = camundaTaskService.getVariables(id)
            val localVariables = camundaTaskService.getVariablesLocal(id)

            val (candidateUsers, candidateGroups) = links.splitToUserGroupCandidates()

            val sender = localVariables[BPMN_TASK_SENDER] as? String

            return ProcTaskDto(
                id = id,
                name = localVariables[BPMN_NAME_ML] as? MLText ?: MLText.EMPTY,
                possibleOutcomes = let {
                    @Suppress("UNCHECKED_CAST")
                    val outcomes = localVariables[BPMN_POSSIBLE_OUTCOMES] as? List<TaskOutcome>
                    return@let outcomes ?: emptyList()
                },
                isDeleted = if (task is TaskEntity) {
                    task.isDeleted
                } else {
                    false
                },
                priority = priority,
                formRef = RecordRef.valueOf(formKey),
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    RecordRef.EMPTY
                } else {
                    RecordRef.create(AppName.EPROC, BpmnProcRecords.ID, processInstanceId)
                },
                documentRef = if (variables[BPMN_DOCUMENT_REF] != null) {
                    RecordRef.valueOf(variables[BPMN_DOCUMENT_REF].toString())
                } else {
                    RecordRef.EMPTY
                },
                documentType = variables[BPMN_DOCUMENT_TYPE] as? String,
                dueDate = dueDate?.toInstant(),
                followUpDate = followUpDate?.toInstant(),
                created = createTime.toInstant(),
                assignee = authorityService.getAuthorityRef(assignee),
                sender = authorityService.getAuthorityRef(sender),
                owner = authorityService.getAuthorityRef(owner),
                candidateUsers = authorityService.getAuthorityRefs(candidateUsers.toList()),
                candidateUsersOriginal = let {
                    @Suppress("UNCHECKED_CAST")
                    val users = localVariables[BPMN_TASK_CANDIDATES_USER_ORIGINAL] as? List<String>
                    return@let users ?: emptyList()
                },
                candidateGroups = authorityService.getAuthorityRefs(candidateGroups.toList()),
                candidateGroupsOriginal = let {
                    @Suppress("UNCHECKED_CAST")
                    val group = localVariables[BPMN_TASK_CANDIDATES_GROUP_ORIGINAL] as? List<String>
                    return@let group ?: emptyList()
                },
                definitionKey = taskDefinitionKey,
                historic = false,
                engineAtts = variables.keys.toList()
            )
        }
    }

    @Cacheable(cacheNames = [PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY], key = "#task.id")
    fun convertHistoricTask(task: HistoricTaskInstance): ProcTaskDto {
        with(task) {
            var nameMl = MLText(name)
            var documentRef = RecordRef.EMPTY

            camundaHistoryService.createHistoricVariableInstanceQuery()
                .taskIdIn(id)
                .variableNameIn(BPMN_NAME_ML)
                .list()
                .forEach {
                    if (it.name == BPMN_NAME_ML) {
                        nameMl = Json.mapper.convert(it.value, MLText::class.java) ?: MLText(name)
                    }
                }

            camundaHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableNameIn(BPMN_DOCUMENT_REF)
                .list()
                .forEach {
                    if (it.name == BPMN_DOCUMENT_REF) {
                        documentRef = RecordRef.valueOf(it.value.toString())
                    }
                }

            return ProcTaskDto(
                id = id,
                priority = priority,
                name = nameMl,
                documentRef = documentRef,
                created = startTime.toInstant(),
                ended = endTime?.toInstant(),
                durationInMillis = durationInMillis,
                dueDate = dueDate?.toInstant(),
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    RecordRef.EMPTY
                } else {
                    RecordRef.create(AppName.EPROC, BpmnProcRecords.ID, processInstanceId)
                },
                historic = true
            )
        }
    }
}
