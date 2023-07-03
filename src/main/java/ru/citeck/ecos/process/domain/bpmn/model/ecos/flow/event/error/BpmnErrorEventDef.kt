package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef

@JsonTypeName("errorEvent")
data class BpmnErrorEventDef(
    override val id: String,

    override var elementId: String = "",

    val errorName: String,

    val errorCode: String = "",
    val errorMessage: String = "",

    val errorCodeVariable: String = "",
    val errorMessageVariable: String = "",
) : BpmnAbstractEventDef() {


    init {
        if (errorName.isBlank()) {
            throw EcosBpmnElementDefinitionException(id, "Error name can't be blank")
        }
    }

}
