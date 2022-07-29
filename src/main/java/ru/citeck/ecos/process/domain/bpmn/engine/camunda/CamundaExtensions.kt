package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SRC_ID_GROUP = "authority-group"

fun DelegateExecution.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
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
