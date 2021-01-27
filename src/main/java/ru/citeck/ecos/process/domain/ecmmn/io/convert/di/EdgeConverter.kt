package ru.citeck.ecos.process.domain.ecmmn.io.convert.di

import ru.citeck.ecos.process.domain.cmmn.model.CMMNEdge
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnEdge
import javax.xml.namespace.QName

class EdgeConverter : CmmnConverter<CMMNEdge, CmmnEdge> {

    companion object {
        const val TYPE = "Edge"
    }

    override fun import(element: CMMNEdge, context: ImportContext): CmmnEdge {

        return CmmnEdge(
            element.id,
            DiagramIOUtils.convertLabel(element.cmmnLabel),
            element.cmmnElementRef?.localPart,
            element.sourceCMMNElementRef?.localPart,
            element.targetCMMNElementRef?.localPart,
            element.waypoint.map { DiagramIOUtils.convertPoint(it) },
            element.isIsStandardEventVisible
        )
    }

    override fun export(element: CmmnEdge, context: ExportContext): CMMNEdge {

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

    override fun getElementType() = TYPE
}
