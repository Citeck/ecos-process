package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SRC_ID_GROUP = "authority-group"

fun DelegateExecution.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateExecution.getNotBlankDocumentRef(): RecordRef {
    val documentFromVar = getDocumentRef()
    if (RecordRef.isEmpty(documentFromVar)) error("Document Ref can't be empty")
    return documentFromVar
}

fun DelegateTask.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateTask.getOutcome(): Outcome {
    val outcomeValue = getVariable("${taskDefinitionKey}$OUTCOME_POSTFIX")?.toString()
        ?: return Outcome.EMPTY

    val outcomeName = Json.mapper.convert(
        getVariable("${taskDefinitionKey}$OUTCOME_NAME_POSTFIX")?.toString() ?: "",
        MLText::class.java
    ) ?: MLText()

    return Outcome(taskDefinitionKey, outcomeValue, outcomeName)
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
