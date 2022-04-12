package ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram


data class BpmnPlaneDef(
    val id: String,
    val elementRef: String,
    val elements: List<BpmnDiagramElementDef>
)
