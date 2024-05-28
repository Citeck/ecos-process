package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_CAMUNDA_ENGINE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FullFulFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.RawFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import javax.annotation.PostConstruct
import kotlin.system.measureTimeMillis

private const val CLASS_IMPL_POSTFIX = "Impl"

private val log = KotlinLogging.logger {}

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementConverter(
    val bpmnProcessService: BpmnProcessService,
    val procDefService: ProcDefService,
    val recordsService: RecordsService
) {

    @PostConstruct
    private fun init() {
        cnv = this
    }
}

private lateinit var cnv: BpmnElementConverter

/**
 * Optimize the conversion of the flow element. Full conversion handled in queue consumer and [toFullFullFlowElement]
 */
fun DelegateExecution.toRawFlowElement(): RawFlowElementEvent {
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

fun RawFlowElementEvent.toFullFullFlowElement(): FullFulFlowElementEvent {
    val fullFulFlowElementEvent: FullFulFlowElementEvent
    val time = measureTimeMillis {
        val processDefinition = processDefinitionId?.let { cnv.bpmnProcessService.getProcessDefinition(it) }
            ?: error(
                "Process definition or flowElement is null. ProcDefId: $processDefinitionId, " +
                    "elementDefId: $elementDefId, executionId: $executionId"
            )

        val rev = cnv.procDefService.getProcessDefRevByDeploymentId(processDefinition.deploymentId)

        fullFulFlowElementEvent = FullFulFlowElementEvent(
            engine = BPMN_CAMUNDA_ENGINE,
            procDefId = rev?.procDefId,
            procDefRef = if (rev?.procDefId?.isNotBlank() == true) {
                EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, rev.procDefId)
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

fun DelegateTask.toTaskEvent(): UserTaskEvent {
    val userTaskEvent: UserTaskEvent
    val time = measureTimeMillis {
        val processDefinition = cnv.bpmnProcessService.getProcessDefinition(processDefinitionId) ?: error(
            "Process definition is null. TaskId: $id, name: $name, executionId: $executionId, " +
                "procInstanceId: $processInstanceId, procDefId: $processDefinitionId"
        )
        val rev = cnv.procDefService.getProcessDefRevByDeploymentId(processDefinition.deploymentId)
        // TODO: fix
//    log.warn {
//        "Process definition revision is null. TaskId: $id, name: $name, executionId: $executionId, " +
//            "procInstanceId: $processInstanceId, procDefId: $processDefinitionId " +
//            " procDefId, procDefRef, procDeploymentVersion will be null"
//    }

        val outcome = getOutcome()
        val userTaskLaInfo = getUserTaskLaInfo()

        userTaskEvent = UserTaskEvent(
            taskId = RecordRef.create(AppName.EPROC, ProcTaskRecords.ID, id),
            engine = BPMN_CAMUNDA_ENGINE,
            form = getFormRef(),
            assignee = assignee,
            roles = getTaskRoles(),
            procDefId = rev?.procDefId,
            procDefRef = if (rev?.procDefId?.isNotBlank() == true) {
                RecordRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, rev.procDefId)
            } else {
                EntityRef.EMPTY
            },
            procDeploymentVersion = rev?.version?.inc(),
            procInstanceId = getProcessInstanceRef(),
            processId = processDefinition.key,
            processRef = if (processDefinition.key.isNotBlank()) {
                RecordRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, processDefinition.key)
            } else {
                EntityRef.EMPTY
            },
            elementDefId = taskDefinitionKey,
            created = createTime?.toInstant(),
            dueDate = dueDate?.toInstant(),
            description = description,
            priority = priority,
            executionId = executionId,
            name = getTitle(),
            comment = getVariableLocal(BPMN_TASK_COMMENT_LOCAL) as? String,
            completedBy = getCompletedBy(),
            outcome = outcome.value,
            outcomeName = outcome.name,
            completedOnBehalfOf = getVariableLocal(BPMN_TASK_COMPLETED_ON_BEHALF_OF) as? String,
            document = getDocumentRef(),
            laEnabled = userTaskLaInfo.laEnabled,
            laNotificationType = userTaskLaInfo.laNotificationType,
            laNotificationTemplate = userTaskLaInfo.laNotificationTemplate,
            laReportEnabled = userTaskLaInfo.laReportEnabled,
            laSuccessReportNotificationTemplate = userTaskLaInfo.laSuccessReportNotificationTemplate,
            laErrorReportNotificationTemplate = userTaskLaInfo.laErrorReportNotificationTemplate
        )
    }

    log.trace {
        "Convert task to user task event in $time ms"
    }

    return userTaskEvent
}
