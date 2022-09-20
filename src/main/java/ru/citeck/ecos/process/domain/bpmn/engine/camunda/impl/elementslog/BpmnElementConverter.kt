package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_CAMUNDA_ENGINE
import ru.citeck.ecos.process.domain.bpmn.COMMENT_VAR
import ru.citeck.ecos.process.domain.bpmn.elements.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.elements.dto.TaskElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getOutcome
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getTitle
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import javax.annotation.PostConstruct

private const val CLASS_IMPL_POSTFIX = "Impl"

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementConverter(
    val bpmnProcService: BpmnProcService
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

    return FlowElementEvent(
        engine = BPMN_CAMUNDA_ENGINE,
        procDefId = processDefinition.key,
        elementType = flowElement.javaClass.simpleName.removeSuffix(CLASS_IMPL_POSTFIX),
        elementDefId = flowElement.id,
        procDeploymentVersion = processDefinition.version.toString(),
        procInstanceId = processInstanceId,
        executionId = id,
        document = getDocumentRef()
    )
}

fun DelegateTask.toTaskElement(): TaskElementEvent {
    val processDefinition = cnv.bpmnProcService.getProcessDefinition(processDefinitionId) ?: error(
        "Process definition is null. TaskId: $id, name: $name, executionId: $executionId, " +
            "procInstanceId: $processInstanceId, procDefId: $processDefinitionId"
    )

    val outcome = getOutcome()

    return TaskElementEvent(
        taskId = id,
        engine = BPMN_CAMUNDA_ENGINE,
        assignee = assignee,
        procDefId = processDefinition.key,
        procDeploymentVersion = processDefinition.version,
        procInstanceId = processInstanceId,
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
