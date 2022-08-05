package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ecos.com.fasterxml.jackson210.annotation.JsonSubTypes
import ecos.com.fasterxml.jackson210.annotation.JsonTypeInfo
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BpmnTimerEventDef::class, name = "timerEvent")
)
abstract class BpmnAbstractEventDef {
    abstract val id: String
}
