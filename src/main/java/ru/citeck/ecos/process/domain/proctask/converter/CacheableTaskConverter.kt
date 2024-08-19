package ru.citeck.ecos.process.domain.proctask.converter

import io.github.oshai.kotlinlogging.KotlinLogging
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
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.proctask.config.PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASKS_DTO_CONVERTER_CACHE_KEY
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

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
                    val outcomes = localVariables[BPMN_POSSIBLE_OUTCOMES] as? List<TaskOutcome> ?: emptyList()
                    return@let outcomes.filter { it.id.isNotBlank() }
                },
                isDeleted = if (task is TaskEntity) {
                    task.isDeleted
                } else {
                    false
                },
                priority = priority,
                formRef = EntityRef.valueOf(formKey),
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    EntityRef.EMPTY
                } else {
                    EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, processInstanceId)
                },
                documentRef = variables[BPMN_DOCUMENT_REF]?.let {
                    EntityRef.valueOf(it.toString())
                } ?: EntityRef.EMPTY,
                documentType = variables[BPMN_DOCUMENT_TYPE] as? String,
                documentTypeRef = variables[BPMN_DOCUMENT_TYPE]?.let {
                    EntityRef.create(AppName.EMODEL, "type", it.toString())
                } ?: EntityRef.EMPTY,
                dueDate = dueDate?.toInstant(),
                followUpDate = followUpDate?.toInstant(),
                lastComment = localVariables[BPMN_LAST_COMMENT_LOCAL] as? String,
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
            var documentRef = EntityRef.EMPTY
            var comment: String? = null
            var lastComment: String? = null

            camundaHistoryService.createHistoricVariableInstanceQuery()
                .taskIdIn(id)
                .variableNameIn(BPMN_NAME_ML, BPMN_TASK_COMMENT_LOCAL, BPMN_LAST_COMMENT_LOCAL)
                .list()
                .forEach {
                    when (it.name) {
                        BPMN_NAME_ML -> {
                            nameMl = Json.mapper.convert(it.value, MLText::class.java) ?: MLText(name)
                        }

                        BPMN_TASK_COMMENT_LOCAL -> {
                            comment = it.value?.toString()
                        }

                        BPMN_LAST_COMMENT_LOCAL -> {
                            lastComment = it.value?.toString()
                        }
                    }
                }

            camundaHistoryService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableNameIn(BPMN_DOCUMENT_REF)
                .list()
                .forEach {
                    if (it.name == BPMN_DOCUMENT_REF) {
                        documentRef = EntityRef.valueOf(it.value.toString())
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
                comment = comment,
                lastComment = lastComment,
                definitionKey = taskDefinitionKey,
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    EntityRef.EMPTY
                } else {
                    EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, processInstanceId)
                },
                historic = true
            )
        }
    }
}
