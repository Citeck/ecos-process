package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.records2.RecordRef

private const val AUTHORITY_GROUP_PREFIX = "GROUP_"

fun DelegateExecution.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun RecordRef.isAuthorityGroupRef(): Boolean {
    return id.startsWith(AUTHORITY_GROUP_PREFIX)
}

fun TaskPriority.toCamundaCode(): Int {
    return when (this) {
        TaskPriority.LOW -> 0
        TaskPriority.MEDIUM -> 2
        TaskPriority.HIGH -> 3
    }
}
