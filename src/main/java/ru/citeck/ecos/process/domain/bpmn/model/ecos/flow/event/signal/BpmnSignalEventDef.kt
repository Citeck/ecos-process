package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_BUSINESS_KEY
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SIGNAL_NAME_MAX_LENGTH = 255

@JsonTypeName("signalEvent")
data class BpmnSignalEventDef(
    override val id: String,

    val eventManualMode: Boolean = false,

    val eventType: EventType? = null,

    val eventFilterByRecordType: FilterEventByRecord? = null,
    val eventFilterByEcosType: EntityRef = EntityRef.EMPTY,
    val eventFilterByRecordVariable: String? = null,
    val eventFilterByPredicate: Predicate? = null,

    val eventModel: Map<String, String> = emptyMap(),


    val manualSignalName: String? = null
) : BpmnAbstractEventDef()

val BpmnSignalEventDef.signalName: String
    get() = let {

        if (it.eventManualMode) {
            if (manualSignalName.isNullOrBlank()) {
                error("Signal name in mandatory for manual mode of Bpmn Signal")
            }
            return manualSignalName
        }

        checkNotNull(eventType) { "Event type is mandatory for Bpmn Signal" }
        checkNotNull(eventFilterByRecordType) { "Event filter by record type is mandatory for Bpmn Signal" }

        if (eventFilterByEcosType.isNotEmpty() && eventFilterByRecordType != FilterEventByRecord.ANY) {
            error("Event filter by Ecos Type supported only for ANY document")
        }

        val filterByRecord: String = when(eventFilterByRecordType) {
            FilterEventByRecord.ANY -> FilterEventByRecord.ANY.name
            FilterEventByRecord.DOCUMENT -> "\${$VAR_BUSINESS_KEY}"
            FilterEventByRecord.DOCUMENT_BY_VARIABLE -> {
                if (eventFilterByRecordVariable.isNullOrBlank()) {
                    error("Document variable is mandatory for filtering event by document")
                }
                "\${$eventFilterByRecordVariable}"
            }
        }

        var result = "${eventType.value}$${filterByRecord}"
        if (eventFilterByEcosType.isNotEmpty()) {
            result += "$${eventFilterByEcosType}"
        }

        if (result.length > SIGNAL_NAME_MAX_LENGTH) {
            error("Signal name is too long, contact you administrator. Name: $result")
        }

        return result;
    }

enum class EventType(val value: String) {
    COMMENT_CREATE("ecos.comment.create"),
    COMMENT_UPDATE("ecos.comment.update"),
    COMMENT_DELETE("ecos.comment.delete");
}

enum class FilterEventByRecord {
    ANY, DOCUMENT, DOCUMENT_BY_VARIABLE
}
