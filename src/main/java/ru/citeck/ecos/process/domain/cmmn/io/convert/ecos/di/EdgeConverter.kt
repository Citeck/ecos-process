package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di

import ru.citeck.ecos.process.domain.cmmn.model.omg.CMMNEdge
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.EdgeDef
import javax.xml.namespace.QName

class EdgeConverter : EcosOmgConverter<EdgeDef, CMMNEdge> {

    override fun import(element: CMMNEdge, context: ImportContext): EdgeDef {

        return EdgeDef(
            element.id,
                DiagramIOUtils.convertLabel(element.cmmnLabel),
            element.cmmnElementRef?.localPart,
            element.sourceCMMNElementRef?.localPart,
            element.targetCMMNElementRef?.localPart,
            element.waypoint.map { DiagramIOUtils.convertPoint(it) },
            element.isIsStandardEventVisible
        )
    }

    override fun export(element: EdgeDef, context: ExportContext): CMMNEdge {

        val edge = CMMNEdge()
        edge.id = element.id

        element.label?.let { edge.cmmnLabel = DiagramIOUtils.convertLabel(it) }
        element.wayPoints.forEach { p -> edge.waypoint.add(DiagramIOUtils.convertPoint(p)) }
        element.elementRef?.let { edge.cmmnElementRef = QName("", it) }
        element.sourceRef?.let { edge.sourceCMMNElementRef = QName("", it) }
        element.targetRef?.let { edge.targetCMMNElementRef = QName("", it) }
        element.isStandardEventVisible?.let { edge.isIsStandardEventVisible = it }

        return edge
    }
}
