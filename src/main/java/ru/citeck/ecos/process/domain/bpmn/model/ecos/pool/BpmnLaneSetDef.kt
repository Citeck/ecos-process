package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

data class BpmnLaneSetDef(
    val id: String,
    val lanes: List<BpmnLaneDef>
)
