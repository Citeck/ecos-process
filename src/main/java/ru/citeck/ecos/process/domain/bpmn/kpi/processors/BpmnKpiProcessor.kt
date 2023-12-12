package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import java.time.Instant

interface BpmnKpiProcessor {

    fun processStartEvent(bpmnEvent: BpmnElementEvent)

    fun processEndEvent(bpmnEvent: BpmnElementEvent)
}

class BpmnElementEvent(
    val procInstanceId: String,
    val processId: String,
    val activityId: String,
    val created: Instant,
    val completed: Instant? = null
)
