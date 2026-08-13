package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.model.lib.authorities.AuthorityType
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.BPMN_CAMUNDA_ENGINE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FullFulFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.RawFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmnla.dto.UserTaskLaInfo
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.dto.getLaCompleted
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import kotlin.system.measureTimeMillis

private const val CLASS_IMPL_POSTFIX = "Impl"

private val log = KotlinLogging.logger {}

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementConverter(
    @Lazy
    private val bpmnProcessService: BpmnProcessService,
    private val procDefService: ProcDefService,
    private val taskDefinitionUtils: TaskDefinitionUtils,
    private val workspaceService: WorkspaceService
) {

    fun toRawFlowElement(dataValue: DataValue): RawFlowElementEvent {
        return Json.mapper.convert(dataValue, RawFlowElementEvent::class.java)
            ?: error("Cannot convert event $this to FlowElementEvent")
    }

    /**
     * Optimize the conversion of the flow element. Full conversion handled in queue consumer and [toFullFulFlowElement]
     */
    fun toRawFlowElement(delegateExecution: DelegateExecution): RawFlowElementEvent {
        with(delegateExecution) {
            val rawFlowElementEvent: RawFlowElementEvent
            val time = measureTimeMillis {
                val flowElement = bpmnModelElementInstance
                check(flowElement != null) {
                    "Flow element is null. ExecutionId: $id, procInstanceId: $processInstanceId, procDefId: $processDefinitionId"
                }

                rawFlowElementEvent = RawFlowElementEvent(
                    processDefinitionId = processDefinitionId,
                    procInstanceId = getProcessInstanceRef(),
                    executionId = id,
                    elementType = flowElement.javaClass.simpleName.removeSuffix(CLASS_IMPL_POSTFIX),
                    elementDefId = flowElement.id,
                    document = getDocumentRef(),
                    time = Instant.now()
                )
            }

            log.trace { "Convert execution to raw flow element in $time ms" }

            return rawFlowElementEvent
        }
    }

    fun toUserTaskEvent(dataValue: DataValue): UserTaskEvent {
        return Json.mapper.convert(dataValue, UserTaskEvent::class.java)
            ?: error("Cannot convert event $this to UserTaskEvent")
    }

    fun toUserTaskEvent(delegateTask: DelegateTask): UserTaskEvent {
        with(delegateTask) {
            val userTaskEvent: UserTaskEvent
            val time = measureTimeMillis {
                val processDefinition = bpmnProcessService.getProcessDefinition(processDefinitionId) ?: error(
                    "Process definition is null. TaskId: $id, name: $name, executionId: $executionId, " +
                        "procInstanceId: $processInstanceId, procDefId: $processDefinitionId"
                )
                val rev = procDefService.getProcessDefRevByDeploymentId(processDefinition.deploymentId)
                val documentRef = getDocumentRef()
                val outcome = getOutcome()
                val (candidateUsers, candidateGroups) = candidates.splitToUserGroupCandidates()

                userTaskEvent = UserTaskEvent(
                    record = documentRef,
                    taskId = EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, id),
                    engine = BPMN_CAMUNDA_ENGINE,
                    form = getFormRef(),
                    assignee = assignee,
                    assigneeRef = assignee?.takeIf { it.isNotBlank() }?.let { AuthorityType.PERSON.getRef(it) },
                    candidateUsers = candidateUsers.toList(),
                    candidateUsersRef = candidateUsers.map { AuthorityType.PERSON.getRef(it) },
                    candidateGroups = candidateGroups.toList(),
                    candidateGroupsRef = candidateGroups.map {
                        AuthorityType.GROUP.getRef(it.removePrefix(AuthGroup.PREFIX))
                    },
                    roles = taskDefinitionUtils.getTaskRoles(delegateTask),
                    procInstanceId = getProcessInstanceRef(),
                    elementDefId = taskDefinitionKey,
                    created = createTime?.toInstant(),
                    dueDate = dueDate?.toInstant(),
                    description = description,
                    priority = priority,
                    executionId = executionId,
                    name = taskDefinitionUtils.getTaskTitle(delegateTask),
                    comment = getVariableLocal(BPMN_TASK_COMMENT_LOCAL) as? String,
                    completedBy = getCompletedBy(),
                    outcome = outcome.value,
                    outcomeName = outcome.name,
                    completedOnBehalfOf = getVariableLocal(BPMN_TASK_COMPLETED_ON_BEHALF_OF) as? String,
                    document = documentRef,
                    isCompletedViaMail = getVariableLocal(BPMN_LA_COMPLETE_KEY) as? Boolean ?: false
                )
                fillProcDefAndProcessRefs(userTaskEvent, rev, processDefinition)
                fillLaInfo(userTaskEvent, taskDefinitionUtils.getUserTaskLaInfo(delegateTask))
            }

            log.trace { "Convert task to user task event in $time ms" }

            return userTaskEvent
        }
    }

    fun toUserTaskEvent(completeData: CompleteTaskData, localVariables: Map<String, Any?>): UserTaskEvent {
        with(completeData) {
            if (task.processDefinitionId.isNullOrBlank()) {
                error(
                    "Process definition is not defined for taskId: ${task.id}, name: ${task.name}, " +
                        "procInstanceId: ${task.processInstanceId}, procDefId: ${task.processDefinitionId}"
                )
            }
            val userTaskEvent: UserTaskEvent
            val time = measureTimeMillis {
                val processDefinition = bpmnProcessService.getProcessDefinition(task.processDefinitionId) ?: error(
                    "Process definition was not found. TaskId: ${task.id}, name: ${task.name}, " +
                        "procInstanceId: ${task.processInstanceId}, procDefId: ${task.processDefinitionId}"
                )
                val rev = procDefService.getProcessDefRevByDeploymentId(processDefinition.deploymentId)
                val userTaskLaInfo = task.definitionKey?.let {
                    taskDefinitionUtils.getUserTaskLaInfo(task.processDefinitionId, it)
                }

                userTaskEvent = UserTaskEvent(
                    record = task.documentRef,
                    taskId = EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id),
                    engine = BPMN_CAMUNDA_ENGINE,
                    form = task.formRef,
                    assignee = task.assignee.getLocalId(),
                    assigneeRef = task.assignee,
                    candidateUsers = task.candidateUsersOriginal,
                    candidateUsersRef = task.candidateUsers,
                    candidateGroups = task.candidateGroupsOriginal,
                    candidateGroupsRef = task.candidateGroups,
                    roles = task.definitionKey?.let {
                        taskDefinitionUtils.getTaskRoles(task.documentRef, task.processDefinitionId, it)
                    } ?: emptyList(),
                    procInstanceId = task.processInstanceId,
                    elementDefId = task.definitionKey,
                    created = task.created,
                    dueDate = task.dueDate,
                    description = null,
                    priority = task.priority,
                    executionId = null,
                    name = task.name,
                    comment = localVariables[BPMN_TASK_COMMENT_LOCAL] as? String,
                    completedBy = localVariables[BPMN_TASK_COMPLETED_BY] as? String,
                    outcome = outcome.value,
                    outcomeName = outcome.name,
                    completedOnBehalfOf = localVariables[BPMN_TASK_COMPLETED_ON_BEHALF_OF] as? String,
                    document = task.documentRef,
                    isCompletedViaMail = getLaCompleted()
                )
                fillProcDefAndProcessRefs(userTaskEvent, rev, processDefinition)
                fillLaInfo(userTaskEvent, userTaskLaInfo)
            }

            log.trace { "Convert complete task data to user task event in $time ms" }

            return userTaskEvent
        }
    }

    private fun fillProcDefAndProcessRefs(
        event: UserTaskEvent,
        rev: ProcDefRevDto?,
        processDefinition: ProcessDefinition
    ) {
        event.procDefId = rev?.procDefId
        event.procDefRef = if (rev?.procDefId?.isNotBlank() == true) {
            val localId = workspaceService.addWsPrefixToId(rev.procDefId, rev.workspace)
            EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
        } else {
            EntityRef.EMPTY
        }
        event.procDeploymentVersion = rev?.version?.inc()
        event.processId = processDefinition.key
        event.processRef = if (processDefinition.key.isNotBlank()) {
            EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, processDefinition.key)
        } else {
            EntityRef.EMPTY
        }
    }

    private fun fillLaInfo(event: UserTaskEvent, userTaskLaInfo: UserTaskLaInfo?) {
        event.laEnabled = userTaskLaInfo?.laEnabled ?: false
        event.laNotificationType = userTaskLaInfo?.laNotificationType
        event.laNotificationTemplate = userTaskLaInfo?.laNotificationTemplate
        event.laManualNotificationTemplateEnabled = userTaskLaInfo?.laManualNotificationTemplateEnabled ?: false
        event.laManualNotificationTemplate = userTaskLaInfo?.laManualNotificationTemplate
        event.laNotificationAdditionalMeta = userTaskLaInfo?.laNotificationAdditionalMeta ?: emptyMap()
        event.laReportEnabled = userTaskLaInfo?.laReportEnabled ?: false
        event.laSuccessReportNotificationTemplate = userTaskLaInfo?.laSuccessReportNotificationTemplate
        event.laErrorReportNotificationTemplate = userTaskLaInfo?.laErrorReportNotificationTemplate
    }

    fun toFullFulFlowElement(rawFlowElementEvent: RawFlowElementEvent): FullFulFlowElementEvent {
        with(rawFlowElementEvent) {
            val fullFulFlowElementEvent: FullFulFlowElementEvent
            val time = measureTimeMillis {
                val processDefinition = processDefinitionId?.let { bpmnProcessService.getProcessDefinition(it) }
                    ?: error(
                        "Process definition or flowElement is null. ProcDefId: $processDefinitionId, " +
                            "elementDefId: $elementDefId, executionId: $executionId"
                    )

                val rev = procDefService.getProcessDefRevByDeploymentId(processDefinition.deploymentId)

                fullFulFlowElementEvent = FullFulFlowElementEvent(
                    engine = BPMN_CAMUNDA_ENGINE,
                    procDefId = rev?.procDefId,
                    procDefRef = if (rev?.procDefId?.isNotBlank() == true) {
                        val localId = workspaceService.addWsPrefixToId(rev.procDefId, rev.workspace)
                        EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
                    } else {
                        EntityRef.EMPTY
                    },
                    elementType = elementType,
                    elementDefId = elementDefId,
                    procDeploymentVersion = rev?.version?.inc(),
                    procInstanceId = procInstanceId,
                    processId = processDefinition.key,
                    processRef = if (processDefinition.key.isNotBlank()) {
                        EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, processDefinition.key)
                    } else {
                        EntityRef.EMPTY
                    },
                    executionId = executionId,
                    document = document,
                    time = time
                )
            }

            log.trace { "Convert execution to flow element in $time ms" }

            return fullFulFlowElementEvent
        }
    }
}
