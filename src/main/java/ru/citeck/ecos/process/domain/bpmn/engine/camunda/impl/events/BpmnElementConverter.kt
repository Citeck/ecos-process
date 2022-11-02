package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_CAMUNDA_ENGINE
import ru.citeck.ecos.process.domain.bpmn.COMMENT_VAR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.constants.AppName
import javax.annotation.PostConstruct

private const val CLASS_IMPL_POSTFIX = "Impl"

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementConverter(
    val bpmnProcService: BpmnProcService,
    val procDefRevRepo: ProcDefRevRepository
) {

    @PostConstruct
    private fun init() {
        cnv = this
    }
}

private lateinit var cnv: BpmnElementConverter

fun DelegateExecution.toFlowElement(): FlowElementEvent {
    val processDefinition = cnv.bpmnProcService.getProcessDefinition(processDefinitionId)
    val flowElement = bpmnModelElementInstance

    if (processDefinition == null || flowElement == null) {
        error(
            "Process definition or flowElement is null. ProcDefId: $processDefinitionId, " +
                "flowId: ${flowElement?.id}, flowName: ${flowElement?.name} executionId: $id"
        )
    }

    val rev = cnv.procDefRevRepo.findByDeploymentId(processDefinition.deploymentId)

    return FlowElementEvent(
        engine = BPMN_CAMUNDA_ENGINE,
        procDefId = processDefinition.key,
        elementType = flowElement.javaClass.simpleName.removeSuffix(CLASS_IMPL_POSTFIX),
        elementDefId = flowElement.id,
        procDeploymentVersion = rev?.version?.inc(),
        procInstanceId = getProcessInstanceRef(),
        executionId = id,
        document = getDocumentRef()
    )
}

fun DelegateTask.toTaskEvent(): UserTaskEvent {
    val processDefinition = cnv.bpmnProcService.getProcessDefinition(processDefinitionId) ?: error(
        "Process definition is null. TaskId: $id, name: $name, executionId: $executionId, " +
            "procInstanceId: $processInstanceId, procDefId: $processDefinitionId"
    )
    val rev = cnv.procDefRevRepo.findByDeploymentId(processDefinition.deploymentId)

    val outcome = getOutcome()

    return UserTaskEvent(
        taskId = RecordRef.create(AppName.EPROC, ProcTaskRecords.ID, id),
        engine = BPMN_CAMUNDA_ENGINE,
        form = getFormRef(),
        assignee = assignee,
        roles = getTaskRoles(),
        procDefId = processDefinition.key,
        procDeploymentVersion = rev?.version?.inc(),
        procInstanceId = getProcessInstanceRef(),
        elementDefId = taskDefinitionKey,
        created = createTime?.toInstant(),
        dueDate = dueDate?.toInstant(),
        description = description,
        priority = priority,
        executionId = executionId,
        name = getTitle(),
        comment = variables[COMMENT_VAR]?.toString(),
        outcome = outcome.value,
        outcomeName = outcome.name,
        document = getDocumentRef()
    )
}