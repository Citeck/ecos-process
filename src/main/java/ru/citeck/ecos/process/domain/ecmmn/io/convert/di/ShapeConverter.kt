package ru.citeck.ecos.process.domain.ecmmn.io.convert.di

import ru.citeck.ecos.process.domain.cmmn.model.CMMNShape
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnShape
import javax.xml.namespace.QName

class ShapeConverter : CmmnConverter<CMMNShape, CmmnShape> {

    companion object {
        const val TYPE = "Shape"
    }

    override fun import(element: CMMNShape, context: ImportContext): CmmnShape {

        return CmmnShape(
            element.id,
            DiagramIOUtils.convertLabel(element.cmmnLabel),
            DiagramIOUtils.convertBoundsNotNull(element.bounds),
            element.cmmnElementRef.localPart,
            element.isIsCollapsed
        )
    }

    override fun export(element: CmmnShape, context: ExportContext): CMMNShape {

        val shape = CMMNShape()
        shape.id = element.id

        element.label?.let { shape.cmmnLabel = DiagramIOUtils.convertLabel(it) }
        shape.isIsCollapsed = element.isCollapsed
        shape.bounds = DiagramIOUtils.convertBounds(element.bounds)
        shape.cmmnElementRef = QName("", element.elementRef)

        return shape
    }

    override fun getElementType() = TYPE
}
