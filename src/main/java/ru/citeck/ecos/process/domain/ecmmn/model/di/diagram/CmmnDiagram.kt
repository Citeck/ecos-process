package ru.citeck.ecos.process.domain.ecmmn.model.di.diagram

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.procdef.model.diagram.Dimension

class CmmnDiagram(
    val id: String,
    val name: MLText,
    val elementRef: String?,
    val size: Dimension?,
    val elements: List<CmmnDiagramElement>
)
