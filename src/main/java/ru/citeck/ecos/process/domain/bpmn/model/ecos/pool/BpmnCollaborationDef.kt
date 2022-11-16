package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

data class BpmnCollaborationDef(
    val id: String,
    val participants: List<BpmnParticipantDef>
)
