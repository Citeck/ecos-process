package ru.citeck.ecos.process.domain.bpmn.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordDeletedEvent
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessAutoDelete(
    eventsService: EventsService,
    private val procDefService: ProcDefService,
    private val bpmnProcessService: BpmnProcessService
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
        val processDef = findProcessDef(eventData.typeRef)
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
            bpmnProcessService.deleteProcessInstance(it)
            log.debug { "Process $it was successfully deleted" }
        }
    }

    private fun findProcessDef(type: EntityRef): ProcDefWithDataDto? {
        val procDef = procDefService.findProcDef(BPMN_PROC_TYPE, type, emptyList()) ?: return null
        return procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procDef.procDefId))
    }

    data class EventData(
        @AttName("record?id")
        var recordRef: EntityRef = EntityRef.EMPTY,

        @AttName("record._type?id")
        var typeRef: EntityRef = EntityRef.EMPTY
    )
}
