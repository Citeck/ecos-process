package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di

import ru.citeck.ecos.process.domain.cmmn.model.omg.Bounds
import ru.citeck.ecos.process.domain.cmmn.model.omg.CMMNLabel
import ru.citeck.ecos.process.domain.cmmn.model.omg.Dimension
import ru.citeck.ecos.process.domain.cmmn.model.omg.Point
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.LabelDef

import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.DimensionDef as EcosDimension
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.BoundsDef as EcosBounds
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.PointDef as EcosPoint

object DiagramIOUtils {

    fun convertPoint(point: Point): EcosPoint {
        return EcosPoint(
            point.x,
            point.y
        )
    }

    fun convertPoint(point: EcosPoint): Point {
        val res = Point()
        res.x = point.x
        res.y = point.y
        return res
    }

    fun convertLabel(label: CMMNLabel?): LabelDef? {

        label ?: return null

        return LabelDef(convertBounds(label.bounds))
    }

    fun convertLabel(label: LabelDef?): CMMNLabel? {

        label ?: return null

        val res = CMMNLabel()
        res.bounds = convertBounds(label.bounds)

        return res
    }

    fun convertBoundsNotNull(bounds: Bounds?): EcosBounds {
        return convertBounds(bounds)!!
    }

    fun convertBounds(bounds: Bounds?): EcosBounds? {

        bounds ?: return null

        return EcosBounds(
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        )
    }

    fun convertBounds(bounds: EcosBounds?): Bounds? {

        bounds ?: return null

        val res = Bounds()
        res.x = bounds.x
        res.y = bounds.y
        res.width = bounds.width
        res.height = bounds.height

        return res
    }

    fun convertDimension(dim: Dimension?): EcosDimension? {

        dim ?: return null

        return EcosDimension(
            dim.width,
            dim.height
        )
    }

    fun convertDimension(dimension: EcosDimension?): Dimension? {

        dimension ?: return null

        val res = Dimension()
        res.width = dimension.width
        res.height = dimension.height

        return res
    }
}
