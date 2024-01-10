package ru.citeck.ecos.process.domain.bpmn.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordDraftStatusChangedEvent
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessAutoStarter(
    eventsService: EventsService,
    private val procDefService: ProcDefService,
    private val bpmnProcessService: BpmnProcessService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        // React on record created without draft
        eventsService.addListener<EventData> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(EventData::class.java)
            withTransactional(true)
            withAction { handleStartProcessEvent(it) }
            withFilter(
                Predicates.and(
                    Predicates.eq("record._type.isSubTypeOf.user-base?bool", true),
                    Predicates.eq("record._isDraft?bool!", false)
                )

            )
        }
        // React on record draft state changed to false
        eventsService.addListener<EventData> {
            withEventType(RecordDraftStatusChangedEvent.TYPE)
            withDataClass(EventData::class.java)
            withTransactional(true)
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
        log.debug { "Received event: $eventData" }

        if (eventData.eventRef == RecordRef.EMPTY) {
            log.warn { "Cannot auto start process for empty eventRef: $eventData" }
            return
        }

        if (eventData.typeRef == RecordRef.EMPTY) {
            log.warn { "Cannot auto start process for empty typeRef: $eventData" }
            return
        }

        startProcessIfRequired(eventData.eventRef, eventData.typeRef)
    }

    private fun startProcessIfRequired(record: EntityRef, type: EntityRef) {
        log.debug { "Processing record: $record, type: $type for auto start process" }

        val procDef = resolveFirstEnabledProcessDefByTypeHierarchy(type)
        if (procDef == null) {
            log.debug { "Process definition not found for $type" }
            return
        }

        if (!procDef.autoStartEnabled) {
            log.debug { "Auto start process disabled for ${procDef.id}" }
            return
        }

        val processVariables = mapOf(
            BPMN_DOCUMENT_REF to record.toString(),
            BPMN_DOCUMENT_TYPE to type.getLocalId()
        )

        log.debug { "Auto start process for ${procDef.id}, vars: $processVariables" }

        // Auto-start process working only for process, which procDef.id = process.id
        bpmnProcessService.startProcess(procDef.id, record.toString(), processVariables)
    }

    private fun resolveFirstEnabledProcessDefByTypeHierarchy(type: EntityRef): ProcDefWithDataDto? {
        val procRev = procDefService.findProcDef(BPMN_PROC_TYPE, type, emptyList()) ?: return null
        return procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procRev.procDefId))
    }

    data class EventData(
        @AttName("record?id")
        var eventRef: EntityRef = RecordRef.EMPTY,

        @AttName("record._type?id")
        var typeRef: EntityRef = RecordRef.EMPTY
    )
}
