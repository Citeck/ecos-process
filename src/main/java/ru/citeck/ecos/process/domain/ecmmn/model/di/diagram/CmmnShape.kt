package ru.citeck.ecos.process.domain.ecmmn.model.di.diagram

import ru.citeck.ecos.process.domain.procdef.model.diagram.Bounds

class CmmnShape(
    val id: String,
    val label: CmmnLabel?,
    val bounds: Bounds,
    val elementRef: String,
    val isCollapsed: Boolean?
)
