package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di

import ru.citeck.ecos.process.domain.cmmn.model.omg.CMMNShape
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.ShapeDef
import javax.xml.namespace.QName

class ShapeConverter : EcosOmgConverter<ShapeDef, CMMNShape> {

    companion object {
        const val TYPE = "Shape"
    }

    override fun import(element: CMMNShape, context: ImportContext): ShapeDef {

        return ShapeDef(
            element.id,
            DiagramIOUtils.convertLabel(element.cmmnLabel),
            DiagramIOUtils.convertBoundsNotNull(element.bounds),
            element.cmmnElementRef.localPart,
            element.isIsCollapsed
        )
    }

    override fun export(element: ShapeDef, context: ExportContext): CMMNShape {

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
