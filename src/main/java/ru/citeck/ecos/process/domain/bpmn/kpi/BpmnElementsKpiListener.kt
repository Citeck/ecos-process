package ru.citeck.ecos.process.domain.bpmn.kpi

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.process.domain.bpmn.elements.config.BPMN_PROCESS_ELEMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.kpi.processors.BpmnElementEvent
import ru.citeck.ecos.process.domain.bpmn.kpi.processors.BpmnKpiProcessor
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.Predicates.empty
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

@Component
class BpmnElementsKpiListener(
    eventsService: EventsService,
    private val bpmnKpiProcessors: List<BpmnKpiProcessor>
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {

        eventsService.addListener<BpmnElementEventData> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(BpmnElementEventData::class.java)
            withFilter(
                eq("typeDef.id", BPMN_PROCESS_ELEMENT_TYPE)
            )
            withAction {
                log.trace { "Received crete event: \n${Json.mapper.toPrettyString(it)}" }
                handleCreateEvent(it)
            }
        }

        eventsService.addListener<BpmnElementEventData> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(BpmnElementEventData::class.java)
            withFilter(
                Predicates.and(
                    eq("typeDef.id", BPMN_PROCESS_ELEMENT_TYPE),
                    eq("diff._has.completed?bool!", true),
                    empty("before.completed")
                )

            )
            withAction {
                log.trace { "Received changed event: \n${Json.mapper.toPrettyString(it)}" }
                handleChangedEvent(it)
            }
        }
    }

    private fun handleCreateEvent(event: BpmnElementEventData) {
        if (event.procInstanceId.isBlank() || event.processId.isBlank() || event.elementDefId.isBlank()) {
            log.warn {
                "Cannot handle create event with empty procInstanceId, processId or elementDefId: " +
                    "${Json.mapper.toPrettyString(event)}"
            }
            return
        }

        val bpmnElementEvent = event.toBpmnElementEvent()
        if (bpmnElementEvent.completed == null) {
            bpmnKpiProcessors.forEach { it.processStartEvent(bpmnElementEvent) }
        } else {
            bpmnKpiProcessors.forEach { it.processStartEvent(bpmnElementEvent) }
            bpmnKpiProcessors.forEach { it.processEndEvent(bpmnElementEvent) }
        }
    }

    private fun handleChangedEvent(event: BpmnElementEventData) {
        if (event.procInstanceId.isBlank() || event.processId.isBlank() || event.elementDefId.isBlank() ||
            event.completed == null
        ) {
            log.warn {
                "Cannot handle changed event with empty procInstanceId, processId or elementDefId: " +
                    "${Json.mapper.toPrettyString(event)}"
            }
            return
        }

        val bpmnElementEvent = event.toBpmnElementEvent()
        bpmnKpiProcessors.forEach { it.processEndEvent(bpmnElementEvent) }
    }

    private data class BpmnElementEventData(
        @AttName("record.procInstanceId")
        var procInstanceId: String = "",

        @AttName("record.processId")
        var processId: String = "",

        @AttName("record.elementDefId")
        var elementDefId: String = "",

        @AttName("record.created")
        var created: Instant = Instant.MIN,

        @AttName("record.completed")
        var completed: Instant? = null,

        @AttName("record.document")
        var document: EntityRef? = EntityRef.EMPTY,

        @AttName("record.document._type?id")
        var documentType: EntityRef? = EntityRef.EMPTY
    ) {

        fun toBpmnElementEvent() = BpmnElementEvent(
            procInstanceId = procInstanceId,
            processId = processId,
            activityId = elementDefId,
            created = created,
            completed = completed,
            document = document ?: EntityRef.EMPTY,
            documentType = documentType ?: EntityRef.EMPTY
        )
    }
}
