package ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram

import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.BoundsDef

class ShapeDef(
    val id: String,
    val label: LabelDef?,
    val bounds: BoundsDef,
    val elementRef: String,
    val isCollapsed: Boolean?
)
