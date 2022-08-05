package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram

import ru.citeck.ecos.process.domain.bpmn.io.convert.toDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.toOmg
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnEdgeDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNEdge
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnEdgeConverter : EcosOmgConverter<BpmnEdgeDef, BPMNEdge> {

    override fun import(element: BPMNEdge, context: ImportContext): BpmnEdgeDef {
        return BpmnEdgeDef(
            id = element.id,
            elementRef = element.bpmnElement.localPart,
            wayPoints = element.waypoint.map { wp -> wp.toDef() }
        )
    }

    override fun export(element: BpmnEdgeDef, context: ExportContext): BPMNEdge {
        return BPMNEdge().apply {
            id = element.id
            bpmnElement = QName("", element.elementRef)

            element.wayPoints.forEach { wp -> waypoint.add(wp.toOmg()) }
        }
    }
}
