package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnPlaneDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNPlane
import ru.citeck.ecos.process.domain.bpmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnPlaneConverter : EcosOmgConverter<BpmnPlaneDef, BPMNPlane> {

    override fun import(element: BPMNPlane, context: ImportContext): BpmnPlaneDef {
        return BpmnPlaneDef(
            id = element.id,
            elementRef = element.bpmnElement.localPart,
            elements = element.diagramElement.map { importElement(it.value, context) }
        )
    }

    private fun importElement(element: DiagramElement, context: ImportContext): BpmnDiagramElementDef {
        val diagramElementDef = context.converters.import(element, context)

        return BpmnDiagramElementDef(
            id = element.id,
            type = diagramElementDef.type,
            data = diagramElementDef.data
        )
    }

    override fun export(element: BpmnPlaneDef, context: ExportContext): BPMNPlane {
        return BPMNPlane().apply {
            id = element.id
            bpmnElement = QName("", element.elementRef)

            element.elements.forEach { diagramElement.add(exportElement(it, context)) }
        }
    }

    private fun exportElement(element: BpmnDiagramElementDef, context: ExportContext): JAXBElement<DiagramElement> {
        val el = context.converters.export<DiagramElement>(element.type, element.data, context)
        return context.converters.convertToJaxb(el)
    }
}
