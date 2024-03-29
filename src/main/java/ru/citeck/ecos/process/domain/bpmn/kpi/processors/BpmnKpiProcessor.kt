package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

interface BpmnKpiProcessor {

    fun processStartEvent(bpmnEvent: BpmnElementEvent)

    fun processEndEvent(bpmnEvent: BpmnElementEvent)
}

class BpmnElementEvent(
    val document: EntityRef,
    val documentType: EntityRef,
    val procInstanceRef: EntityRef,
    val processRef: EntityRef,
    val procDefRef: EntityRef,
    val activityId: String,
    val created: Instant,
    val completed: Instant? = null
)
