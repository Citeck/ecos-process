package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TErrorEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnErrorEventDefinitionConverter : EcosOmgConverter<BpmnErrorEventDef, TErrorEventDefinition> {

    override fun import(element: TErrorEventDefinition, context: ImportContext): BpmnErrorEventDef {
        return BpmnErrorEventDef(
            id = element.id
        )
    }

    override fun export(element: BpmnErrorEventDef, context: ExportContext): TErrorEventDefinition {
        return TErrorEventDefinition().apply {
            id = element.id
        }
    }
}
