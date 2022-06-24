package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time.TimeValue

@JsonTypeName("timerEvent")
data class BpmnTimerEventDef(
    override val id: String,

    val value: TimeValue,
) : BpmnAbstractEventDef()
