package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName

@JsonTypeName("errorEvent")
data class BpmnErrorEventDef(
    override val id: String,

    override var elementId: String = "",
) : BpmnAbstractEventDef()
