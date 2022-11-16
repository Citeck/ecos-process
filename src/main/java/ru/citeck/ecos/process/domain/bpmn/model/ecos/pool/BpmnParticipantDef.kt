package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

import ru.citeck.ecos.commons.data.MLText

data class BpmnParticipantDef(
    val id: String,
    val name: MLText,
    val processRef: String
)
