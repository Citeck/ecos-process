package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BpmnTimerEventDef::class, name = "timerEvent"),
    JsonSubTypes.Type(value = BpmnSignalEventDef::class, name = "signalEvent"),
    JsonSubTypes.Type(value = BpmnErrorEventDef::class, name = "errorEvent"),
    JsonSubTypes.Type(value = BpmnSignalEventDef::class, name = "signalEvent"),
    JsonSubTypes.Type(value = BpmnTerminateEventDef::class, name = "terminateEvent"),
    JsonSubTypes.Type(value = BpmnConditionalEventDef::class, name = "conditionalEvent")
)
abstract class BpmnAbstractEventDef {
    abstract val id: String
    abstract var elementId: String
}
