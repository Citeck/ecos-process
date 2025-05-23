package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.convertManualStatusSelectionToPredicate
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.FilterEventByRecord
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.StatusChangeType
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignalEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnSignalEventDefinitionConverter : EcosOmgConverter<BpmnSignalEventDef, TSignalEventDefinition> {

    override fun import(element: TSignalEventDefinition, context: ImportContext): BpmnSignalEventDef {
        val isManualMode = element.otherAttributes[BPMN_PROP_EVENT_MANUAL_MODE]?.toBoolean() == true

        val eventType = if (isManualMode) {
            null
        } else {
            val eventTypeAttr = element.otherAttributes[BPMN_PROP_EVENT_TYPE]
            if (eventTypeAttr.isNullOrBlank()) {
                error("Event type is not specified: ${element.id}")
            }

            EcosEventType.valueOf(eventTypeAttr)
        }
        val statusChangeType = element.otherAttributes[BPMN_PROP_STATUS_CHANGE_TYPE]?.takeIf { it.isNotBlank() }?.let {
            StatusChangeType.valueOf(it)
        }
        val manualStatus = element.otherAttributes[BPMN_PROP_MANUAL_STATUS]

        val signal = BpmnSignalEventDef(
            id = element.id,
            eventManualMode = isManualMode,
            manualSignalName = if (eventType == EcosEventType.USER_EVENT) {
                element.otherAttributes[BPMN_PROP_USER_EVENT]
            } else {
                element.otherAttributes[BPMN_PROP_MANUAL_SIGNAL_NAME]
            },
            statusChangeType = statusChangeType,
            manualStatus = manualStatus,
            eventType = eventType,
            eventFilterByRecordType = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_RECORD_TYPE]?.let {
                FilterEventByRecord.valueOf(it)
            } ?: error("Event filter by record is mandatory for Bpmn Signal: ${element.id}"),
            eventFilterByEcosType = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_ECOS_TYPE]?.let {
                if (it.isBlank()) {
                    return@let EntityRef.EMPTY
                }

                val typeRef = EntityRef.valueOf(it)
                typeRef.withSourceId("type")
            } ?: EntityRef.EMPTY,
            eventFilterByRecordVariable = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_RECORD_VARIABLE],
            eventFilterByPredicate = if (eventType == EcosEventType.RECORD_STATUS_CHANGED && statusChangeType != null &&
                manualStatus != null && manualStatus.isNotBlank()
            ) {
                convertManualStatusSelectionToPredicate(statusChangeType, manualStatus)
            } else {
                element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_PREDICATE]?.let {
                    Json.mapper.convert(it, Predicate::class.java)
                }
            },
            eventModel = element.otherAttributes[BPMN_PROP_EVENT_MODEL]?.let {
                Json.mapper.readMap(it, String::class.java, String::class.java)
            } ?: emptyMap()
        )

        context.bpmnSignalEventDefs.add(signal)

        return signal
    }

    override fun export(element: BpmnSignalEventDef, context: ExportContext): TSignalEventDefinition {
        val signalId = context.bpmnSignalsByNames[element.signalName]?.id
            ?: error("Signal with name ${element.signalName} not found")

        return TSignalEventDefinition().apply {
            id = element.id
            signalRef = QName("", signalId)
        }
    }
}
