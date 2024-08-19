package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

@JsonTypeName("errorEvent")
data class BpmnErrorEventDef(
    override val id: String,

    override var elementId: String = "",

    val errorName: String,

    val errorCode: String = "",
    val errorMessage: String = "",

    val errorCodeVariable: String = "",
    val errorMessageVariable: String = "",
) : BpmnAbstractEventDef(), Validated {

    override fun validate() {
        if (errorName.isBlank()) {
            throw EcosBpmnElementDefinitionException(id, "Error name can't be blank")
        }
    }
}
