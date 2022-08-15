package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram

import ru.citeck.ecos.process.domain.bpmn.io.convert.toDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.toOmg
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnShapeDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNShape
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnShapeConverter : EcosOmgConverter<BpmnShapeDef, BPMNShape> {

    override fun import(element: BPMNShape, context: ImportContext): BpmnShapeDef {
        return BpmnShapeDef(
            id = element.id,
            elementRef = element.bpmnElement.localPart,
            bounds = element.bounds.toDef(),
            expanded = element.isIsExpanded
        )
    }

    override fun export(element: BpmnShapeDef, context: ExportContext): BPMNShape {
        return BPMNShape().apply {
            id = element.id
            bpmnElement = QName("", element.elementRef)
            bounds = element.bounds.toOmg()
            isIsExpanded = element.expanded
        }
    }
}
