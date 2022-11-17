package ru.citeck.ecos.process.domain.bpmn.model.ecos.signal

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef

data class BpmnSignalDef(
    val id: String,
    val name: String,
    val eventDef: BpmnSignalEventDef
)
