package ru.citeck.ecos.process.domain.proctask.converter

import io.github.oshai.kotlinlogging.KotlinLogging
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
import ru.citeck.ecos.process.domain.proctask.service.HistoricTaskLoader
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * @author Roman Makarskiy
 */
@Component
class CacheableTaskConverter(
    private val camundaTaskService: TaskService,
    private val historicTaskLoader: HistoricTaskLoader,
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
        return convertTaskUncached(task)
    }

    /**
     * Read the current engine state without consulting or populating the shared DTO cache.
     */
    fun convertTaskUncached(task: Task): ProcTaskDto {
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
                processDefinitionId = processDefinitionId,
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
            val (process, local) = historicTaskLoader.variables(task)
            val nameMl = local[BPMN_NAME_ML]?.let { Json.mapper.convert(it, MLText::class.java) } ?: MLText(name)
            val documentRef = EntityRef.valueOf(process[BPMN_DOCUMENT_REF]?.toString())
            val documentType = process[BPMN_DOCUMENT_TYPE] as? String
            val snapshots = local.filterKeys { it.startsWith("_doc_") }

            return ProcTaskDto(
                id = id,
                priority = priority,
                name = nameMl,
                documentRef = documentRef,
                created = startTime.toInstant(),
                ended = endTime?.toInstant(),
                durationInMillis = durationInMillis,
                dueDate = dueDate?.toInstant(),
                comment = local[BPMN_TASK_COMMENT_LOCAL] as? String,
                lastComment = local[BPMN_LAST_COMMENT_LOCAL] as? String,
                assignee = authorityService.getAuthorityRef(assignee),
                documentType = documentType,
                documentTypeRef = documentType?.let { EntityRef.create(AppName.EMODEL, "type", it) } ?: EntityRef.EMPTY,
                completedBy = local[BPMN_TASK_COMPLETED_BY] as? String,
                deleteReason = deleteReason,
                historicVariables = snapshots,
                definitionKey = taskDefinitionKey,
                processInstanceId = if (processInstanceId.isNullOrBlank()) {
                    EntityRef.EMPTY
                } else {
                    EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, processInstanceId)
                },
                processDefinitionId = processDefinitionId,
                historic = true
            )
        }
    }
}
