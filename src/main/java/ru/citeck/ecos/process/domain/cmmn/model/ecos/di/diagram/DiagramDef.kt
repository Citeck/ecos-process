package ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.math.DimensionDef

class DiagramDef(
    val id: String,
    val name: MLText,
    val elementRef: String?,
    val size: DimensionDef?,
    val elements: List<DiagramElementDef>
)
