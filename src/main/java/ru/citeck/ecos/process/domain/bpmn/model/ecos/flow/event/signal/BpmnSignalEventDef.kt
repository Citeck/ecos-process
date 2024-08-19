package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_BUSINESS_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.webapp.api.entity.EntityRef

@JsonTypeName("signalEvent")
data class BpmnSignalEventDef(
    override val id: String,

    override var elementId: String = "",

    val eventManualMode: Boolean = false,

    val eventType: EcosEventType? = null,

    val eventFilterByRecordType: FilterEventByRecord,
    val eventFilterByEcosType: EntityRef = EntityRef.EMPTY,
    val eventFilterByRecordVariable: String? = null,
    val eventFilterByPredicate: Predicate? = null,

    val eventModel: Map<String, String> = emptyMap(),

    val manualSignalName: String? = null
) : BpmnAbstractEventDef(), Validated {

    val signalName: String
        get() = let {

            val finalEventName = if (it.eventManualMode) {
                if (manualSignalName.isNullOrBlank()) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Signal name in mandatory for manual mode of Bpmn Signal."
                    )
                }
                manualSignalName
            } else {
                eventType?.name ?: throw EcosBpmnElementDefinitionException(
                    id,
                    "Event type is mandatory for Bpmn Signal."
                )
            }

            val filterByRecord: String = when (eventFilterByRecordType) {
                FilterEventByRecord.ANY -> ComposedEventName.RECORD_ANY
                FilterEventByRecord.DOCUMENT -> "\${$BPMN_BUSINESS_KEY}"
                FilterEventByRecord.DOCUMENT_BY_VARIABLE -> {
                    if (eventFilterByRecordVariable.isNullOrBlank()) {
                        throw EcosBpmnElementDefinitionException(
                            id,
                            "Document variable is mandatory for filtering event by document."
                        )
                    }
                    "\${$eventFilterByRecordVariable}"
                }
            }

            val filterByEcosType: String = if (eventFilterByEcosType == EntityRef.EMPTY) {
                ComposedEventName.TYPE_ANY
            } else {
                eventFilterByEcosType.toString()
            }

            return ComposedEventName(
                event = finalEventName,
                record = filterByRecord,
                type = filterByEcosType
            ).toComposedStringWithPredicateChecksum(eventFilterByPredicate ?: VoidPredicate.INSTANCE)
        }

    override fun validate() {

        if (eventFilterByEcosType.isNotEmpty() && eventFilterByRecordType != FilterEventByRecord.ANY) {
            throw EcosBpmnElementDefinitionException(id, "Event filter by Ecos Type supported only for ANY document.")
        }

        when (eventType) {
            EcosEventType.RECORD_CREATED -> {
                if (eventFilterByEcosType == EntityRef.EMPTY &&
                    (eventFilterByPredicate == null || eventFilterByPredicate == VoidPredicate.INSTANCE)
                ) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Event filter by Ecos Type or Predicate required for RECORD_CREATED event"
                    )
                }
            }

            else -> {
                // do nothing
            }
        }
    }
}

enum class FilterEventByRecord {
    ANY,
    DOCUMENT,
    DOCUMENT_BY_VARIABLE
}
