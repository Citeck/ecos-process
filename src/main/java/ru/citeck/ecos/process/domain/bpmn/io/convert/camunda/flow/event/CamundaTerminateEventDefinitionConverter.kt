package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnTerminateEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTerminateEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaTerminateEventDefinitionConverter : EcosOmgConverter<BpmnTerminateEventDef, TTerminateEventDefinition> {
    override fun import(element: TTerminateEventDefinition, context: ImportContext): BpmnTerminateEventDef {
        error("Not supported")
    }

    override fun export(element: BpmnTerminateEventDef, context: ExportContext): TTerminateEventDefinition {
        return TTerminateEventDefinition().apply {
            id = element.id
        }
    }
}
