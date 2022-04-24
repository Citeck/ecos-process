package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText

data class BpmnExclusiveGatewayDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),
)
