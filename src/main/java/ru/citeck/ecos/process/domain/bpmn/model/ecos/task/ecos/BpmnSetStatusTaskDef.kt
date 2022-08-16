package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException

const val ECOS_TASK_SET_STATUS = "setStatus"

@JsonTypeName(ECOS_TASK_SET_STATUS)
data class BpmnSetStatusTaskDef(
    val status: String
) : BpmnAbstractEcosTaskDef() {

    init {
        if (status.isBlank()) {
            throw EcosBpmnDefinitionException("On set status task def, status cannot be blank. $this")
        }
    }
}
