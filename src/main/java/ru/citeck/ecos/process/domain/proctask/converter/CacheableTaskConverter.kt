package ru.citeck.ecos.process.domain.proctask.converter

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.task.IdentityLinkType
import org.camunda.bpm.engine.task.Task
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_POSSIBLE_OUTCOMES
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

    @Cacheable(cacheNames = [PROC_TASKS_DTO_CONVERTER_CACHE_KEY], key = "#task.id")
    fun convertTask(task: Task): ProcTaskDto {
        with(task) {
            val links = camundaTaskService.getIdentityLinksForTask(id)
            val variables = camundaTaskService.getVariables(id)
            val localVariables = camundaTaskService.getVariablesLocal(id)

            val candidateUsers = mutableSetOf<String>()
            val candidateGroups = mutableSetOf<String>()

            links.forEach {
                if (it.type == IdentityLinkType.CANDIDATE) {
                    it.userId?.let { userId ->
                        candidateUsers.add(userId)
                    }
                    it.groupId?.let { groupId ->
                        candidateGroups.add(groupId)
                    }
                }
            }

            return ProcTaskDto(
                id = id,
                name = localVariables[VAR_NAME_ML] as? MLText ?: MLText.EMPTY,
                possibleOutcomes = let {
                    @Suppress("UNCHECKED_CAST")
                    val outcomes = localVariables[VAR_POSSIBLE_OUTCOMES] as? List<TaskOutcome>
                    return@let outcomes ?: emptyList()
                },
                priority = priority,
                formRef = RecordRef.valueOf(formKey),
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    RecordRef.EMPTY
                } else {
                    RecordRef.create(AppName.EPROC, BpmnProcRecords.ID, processInstanceId)
                },
                documentRef = if (variables[VAR_DOCUMENT_REF] != null) {
                    RecordRef.valueOf(variables[VAR_DOCUMENT_REF].toString())
                } else {
                    RecordRef.EMPTY
                },
                dueDate = dueDate?.toInstant(),
                created = createTime.toInstant(),
                assignee = authorityService.getAuthorityRef(assignee),
                candidateUsers = authorityService.getAuthorityRefs(candidateUsers.toList()),
                candidateGroups = authorityService.getAuthorityRefs(candidateGroups.toList()),
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
                .variableNameIn(VAR_NAME_ML)
                .list()
                .forEach {
                    if (it.name == VAR_NAME_ML) {
                        nameMl = Json.mapper.convert(it.value, MLText::class.java) ?: MLText(name)
                    }
                }

            camundaHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableNameIn(VAR_DOCUMENT_REF)
                .list()
                .forEach {
                    if (it.name == VAR_DOCUMENT_REF) {
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
