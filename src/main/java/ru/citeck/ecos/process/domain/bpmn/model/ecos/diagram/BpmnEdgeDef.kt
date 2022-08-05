package ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram

import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.PointDef

data class BpmnEdgeDef(
    val id: String,
    val elementRef: String,
    val wayPoints: List<PointDef> = emptyList(),
)
