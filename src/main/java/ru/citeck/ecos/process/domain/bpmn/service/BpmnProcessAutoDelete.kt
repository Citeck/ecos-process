package ru.citeck.ecos.process.domain.bpmn.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordDeletedEvent
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.CachedFirstEnabledProcessDefFinder
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessAutoDelete(
    eventsService: EventsService,
    private val bpmnProcessService: BpmnProcessService,
    private val cachedFirstEnabledProcessDefFinder: CachedFirstEnabledProcessDefFinder
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<EventData> {
            withTransactional(true)
            withEventType(RecordDeletedEvent.TYPE)
            withDataClass(EventData::class.java)
            withAction { handleDeleteProcessEvent(it) }
            withFilter(
                Predicates.and(
                    Predicates.eq("record._type.isSubTypeOf.user-base?bool", true),
                    Predicates.eq("record._isDraft?bool!", false)
                )
            )
        }
    }

    private fun handleDeleteProcessEvent(eventData: EventData) {
        log.debug { "Received event: $eventData" }
        val processDef = cachedFirstEnabledProcessDefFinder.find(eventData.typeRef.toString())
        if (processDef == null) {
            log.debug { "Process definition not found for ${eventData.typeRef}" }
            return
        }

        if (!processDef.autoDeleteEnabled) {
            log.debug { "Auto delete process disabled for ${processDef.id}" }
            return
        }

        val processList = bpmnProcessService.getProcessInstancesForBusinessKey(eventData.recordRef.toString()).map {
            it.processInstanceId
        }
        processList.forEach {
            bpmnProcessService.deleteProcessInstance(processInstanceId = it, reason = "document has been deleted")
            log.debug { "Process $it was successfully deleted" }
        }
    }

    data class EventData(
        @AttName("record?id")
        var recordRef: EntityRef = EntityRef.EMPTY,

        @AttName("record._type?id")
        var typeRef: EntityRef = EntityRef.EMPTY
    )
}
