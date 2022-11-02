package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.EventType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.FilterEventByRecord
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.signalName
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignalEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnSignalEventDefinitionConverter : EcosOmgConverter<BpmnSignalEventDef, TSignalEventDefinition> {

    override fun import(element: TSignalEventDefinition, context: ImportContext): BpmnSignalEventDef {
        val signal = BpmnSignalEventDef(
            id = element.id,
            eventManualMode = element.otherAttributes[BPMN_PROP_EVENT_MANUAL_MODE]?.toBoolean()
                ?: false,
            manualSignalName = element.otherAttributes[BPMN_PROP_MANUAL_SIGNAL_NAME],
            eventType = element.otherAttributes[BPMN_PROP_EVENT_TYPE]?.let { EventType.valueOf(it) },
            eventFilterByRecordType = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_RECORD_TYPE]?.let {
                if (it.isBlank()) {
                    error("Event filter by record is mandatory for Bpmn Signal")
                }
                FilterEventByRecord.valueOf(it)
            },
            eventFilterByEcosType = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_ECOS_TYPE]?.let {
                if (it.isBlank()) {
                    return@let EntityRef.EMPTY
                }

                val typeRef = EntityRef.valueOf(it)
                typeRef.withSourceId("type")
            } ?: EntityRef.EMPTY,
            eventFilterByRecordVariable = element.otherAttributes[BPMN_PROP_EVENT_FILTER_BY_RECORD_VARIABLE],
        )

        context.bpmnSignalNames.add(signal.signalName)

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