package ru.citeck.ecos.process.domain.bpmn.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

@Component
class BpmnProcessAutoStarter(
    eventsService: EventsService,
    private val procDefService: ProcDefService,
    private val bpmnProcService: BpmnProcService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<EventData> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(EventData::class.java)
            withAction { startProcessIfRequired(it) }
        }
    }

    private fun startProcessIfRequired(eventData: EventData) {
        val procRev = procDefService.findProcDef(BPMN_PROC_TYPE, eventData.typeRef, emptyList()) ?: return
        val procDef = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procRev.procDefId)) ?: return
        if (!procDef.autoStartEnabled) return

        if (eventData.eventRef == RecordRef.EMPTY) {
            log.warn { "Cannot auto start process for empty eventRef: $eventData" }
            return
        }

        val documentRef = eventData.eventRef.toString()
        val processVariables = mapOf(VAR_DOCUMENT_REF to documentRef)

        log.debug { "Auto start process for ${procDef.id}, vars: $processVariables" }
        bpmnProcService.startProcess(procDef.id, documentRef, processVariables)
    }

    data class EventData(
        @AttName("record?id")
        var eventRef: RecordRef = RecordRef.EMPTY,

        @AttName("record._type?id")
        var typeRef: RecordRef = RecordRef.EMPTY
    )
}
