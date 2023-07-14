package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnMessageFlowDef

data class BpmnCollaborationDef(
    val id: String,
    val participants: List<BpmnParticipantDef>,
    val messageFlows: List<BpmnMessageFlowDef>
)
