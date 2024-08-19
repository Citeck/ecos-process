package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("terminateEvent")
data class BpmnTerminateEventDef(
    override val id: String,

    override var elementId: String = ""
) : BpmnAbstractEventDef()
