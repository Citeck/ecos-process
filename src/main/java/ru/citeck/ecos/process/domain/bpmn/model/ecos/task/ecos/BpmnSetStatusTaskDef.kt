package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

const val ECOS_TASK_SET_STATUS = "setStatus"

@JsonTypeName(ECOS_TASK_SET_STATUS)
data class BpmnSetStatusTaskDef(
    val status: String
) : BpmnAbstractEcosTaskDef(),
    Validated {

    override fun validate() {
        if (status.isBlank()) {
            throw EcosBpmnDefinitionException("On set status task def, status cannot be blank. $this")
        }
    }
}
