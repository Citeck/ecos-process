package ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram

import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.BoundsDef

data class BpmnShapeDef(
    val id: String,
    val elementRef: String,
    val bounds: BoundsDef,
    val expanded: Boolean
)
