package ru.citeck.ecos.process.domain.ecmmn.model.di.diagram

import ru.citeck.ecos.process.domain.procdef.model.diagram.Point

class CmmnEdge(
    val id: String,
    val label: CmmnLabel?,
    val elementRef: String?,
    val sourceRef: String?,
    val targetRef: String?,
    val wayPoints: List<Point>,
    val isStandardEventVisible: Boolean?
)
