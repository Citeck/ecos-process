package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence

data class BpmnSequenceFlowDef(
    val id: String,
    val sourceRef: String,
    val targetRef: String
)
