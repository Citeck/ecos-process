package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram

import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnPlaneDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNPlane
import ru.citeck.ecos.process.domain.bpmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class CamundaPlaneConverter : EcosOmgConverter<BpmnPlaneDef, BPMNPlane> {

    override fun import(element: BPMNPlane, context: ImportContext): BpmnPlaneDef {
        error("Not supported")
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
