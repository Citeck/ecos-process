package ru.citeck.ecos.process.domain.bpmn.process

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordDraftStatusChangedEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKSPACE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

@Component
class BpmnProcessAutoStarter(
    eventsService: EventsService,
    private val bpmnProcessService: BpmnProcessService,
    private val cachedFirstEnabledProcessDefFinder: CachedFirstEnabledProcessDefFinder
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        // React on user-base record created without draft
        eventsService.addListener<EventData> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(EventData::class.java)
            withTransactional(false)
            withAction { handleStartProcessEvent(it) }
            withFilter(
                Predicates.and(
                    Predicates.eq("record._type.isSubTypeOf.user-base?bool", true),
                    Predicates.eq("record._isDraft?bool!", false)
                )

            )
        }
        // React on user-base record draft state changed to false
        eventsService.addListener<EventData> {
            withEventType(RecordDraftStatusChangedEvent.TYPE)
            withDataClass(EventData::class.java)
            withTransactional(false)
            withAction { handleStartProcessEvent(it) }
            withFilter(
                Predicates.and(
                    Predicates.eq("record._type.isSubTypeOf.user-base?bool", true),
                    Predicates.eq("after?bool", false)
                )

            )
        }
    }

    private fun handleStartProcessEvent(eventData: EventData) {
        val time = measureTimeMillis {
            log.debug { "Received event: $eventData" }

            if (eventData.eventRef == EntityRef.EMPTY) {
                log.warn { "Cannot auto start process for empty eventRef: $eventData" }
                return
            }

            if (eventData.typeRef == EntityRef.EMPTY) {
                log.warn { "Cannot auto start process for empty typeRef: $eventData" }
                return
            }

            startProcessIfRequired(eventData.eventRef, eventData.workspace, eventData.typeRef)
        }

        log.trace { "Handled start process event for record ${eventData.eventRef} in $time ms" }
    }

    private fun startProcessIfRequired(record: EntityRef, workspace: String, type: EntityRef) {
        log.debug { "Processing record: $record, type: $type for auto start process" }

        val procDef = cachedFirstEnabledProcessDefFinder.find(workspace, type.toString())
        if (procDef == null) {
            log.debug { "Process definition not found for $type" }
            return
        }

        if (!procDef.autoStartEnabled) {
            log.debug { "Auto start process disabled for ${procDef.id}" }
            return
        }

        val processVariables = mapOf(
            BPMN_WORKSPACE to procDef.workspace,
            BPMN_DOCUMENT_REF to record.toString(),
            BPMN_DOCUMENT_TYPE to type.getLocalId()
        )

        log.debug { "Auto start process for ${procDef.id}, vars: $processVariables" }

        // Auto-start process working only for process, which procDef.id = process.id
        bpmnProcessService.startProcessAsync(
            StartProcessRequest(
                procDef.workspace,
                procDef.id,
                record.toString(),
                processVariables
            )
        )
    }

    data class EventData(
        @AttName("record?id")
        var eventRef: EntityRef = EntityRef.EMPTY,

        @AttName("record._type?id")
        var typeRef: EntityRef = EntityRef.EMPTY,

        @AttName("record._workspace?localId!")
        var workspace: String
    )
}
