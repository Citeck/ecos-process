package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import ru.citeck.ecos.records2.RecordRef

private const val AUTHORITY_GROUP_PREFIX = "GROUP_"

fun DelegateExecution.getDocument(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT) as String?
    return RecordRef.valueOf(documentVar)
}

fun RecordRef.isAuthorityGroupRef(): Boolean {
    return id.startsWith(AUTHORITY_GROUP_PREFIX)
}
