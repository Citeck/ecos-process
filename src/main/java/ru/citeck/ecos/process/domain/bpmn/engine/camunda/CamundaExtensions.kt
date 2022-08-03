package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SRC_ID_GROUP = "authority-group"

fun DelegateExecution.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateTask.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateTask.getOutcome(): Outcome {
    val outComeValue = getVariable("${taskDefinitionKey}$OUTCOME_POSTFIX")?.toString()
        ?: return Outcome.EMPTY
    return Outcome(taskDefinitionKey, outComeValue)
}

fun EntityRef.isAuthorityGroupRef(): Boolean {
    return getSourceId() == SRC_ID_GROUP
}

fun TaskPriority.toCamundaCode(): Int {
    return when (this) {
        TaskPriority.LOW -> 3
        TaskPriority.MEDIUM -> 2
        TaskPriority.HIGH -> 1
    }
}

fun ActivityImpl.addTaskListener(eventName: String, listener: TaskListener) {
    val activityBehavior = activityBehavior
    if (activityBehavior is UserTaskActivityBehavior) {
        activityBehavior.taskDefinition.addTaskListener(
            eventName,
            listener
        )
    }
}
