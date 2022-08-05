package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnPlaneDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNDiagram
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnDiagramConverter : EcosOmgConverter<BpmnDiagramDef, BPMNDiagram> {

    override fun import(element: BPMNDiagram, context: ImportContext): BpmnDiagramDef {
        return BpmnDiagramDef(
            id = element.id,
            plane = context.converters.import(element.bpmnPlane, BpmnPlaneDef::class.java, context).data
        )
    }

    override fun export(element: BpmnDiagramDef, context: ExportContext): BPMNDiagram {
        val diagram = BPMNDiagram()

        diagram.id = element.id
        diagram.bpmnPlane = context.converters.export(element.plane)

        return diagram
    }
}
