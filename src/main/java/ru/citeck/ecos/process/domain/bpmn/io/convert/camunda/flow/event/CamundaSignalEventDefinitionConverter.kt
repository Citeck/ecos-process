package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.signalName
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignalEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaSignalEventDefinitionConverter : EcosOmgConverter<BpmnSignalEventDef, TSignalEventDefinition> {

    override fun import(element: TSignalEventDefinition, context: ImportContext): BpmnSignalEventDef {
        error("Not supported")
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
