package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda

import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNDiagram
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaDiagramConverter : EcosOmgConverter<BpmnDiagramDef, BPMNDiagram> {

    override fun import(element: BPMNDiagram, context: ImportContext): BpmnDiagramDef {
        error("Not supported")
    }

    override fun export(element: BpmnDiagramDef, context: ExportContext): BPMNDiagram {
        return BPMNDiagram().apply {
            id = element.id
            bpmnPlane = context.converters.export(element.plane)
        }
    }
}
