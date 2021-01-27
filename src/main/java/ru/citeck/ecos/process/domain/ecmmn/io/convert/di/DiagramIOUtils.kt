package ru.citeck.ecos.process.domain.ecmmn.io.convert.di

import ru.citeck.ecos.process.domain.cmmn.model.Bounds
import ru.citeck.ecos.process.domain.cmmn.model.CMMNLabel
import ru.citeck.ecos.process.domain.cmmn.model.Dimension
import ru.citeck.ecos.process.domain.cmmn.model.Point
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnLabel

import ru.citeck.ecos.process.domain.procdef.model.diagram.Dimension as EcosDimension
import ru.citeck.ecos.process.domain.procdef.model.diagram.Bounds as EcosBounds
import ru.citeck.ecos.process.domain.procdef.model.diagram.Point as EcosPoint

object DiagramIOUtils {

    fun convertPoint(point: Point): EcosPoint {
        return EcosPoint(
            point.x,
            point.y
        )
    }

    fun convertPoint(point: EcosPoint): Point {
        val res = ru.citeck.ecos.process.domain.cmmn.model.Point()
        res.x = point.x
        res.y = point.y
        return res
    }

    fun convertLabel(label: CMMNLabel?): CmmnLabel? {

        label ?: return null

        return CmmnLabel(convertBounds(label.bounds))
    }

    fun convertLabel(label: CmmnLabel?): CMMNLabel? {

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

        val res = ru.citeck.ecos.process.domain.cmmn.model.Dimension()
        res.width = dimension.width
        res.height = dimension.height

        return res
    }
}
