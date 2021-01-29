package ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram

import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.PointDef

class EdgeDef(
        val id: String,
        val label: LabelDef?,
        val elementRef: String?,
        val sourceRef: String?,
        val targetRef: String?,
        val wayPoints: List<PointDef>,
        val isStandardEventVisible: Boolean?
)
