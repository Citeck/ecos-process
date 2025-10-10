package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class BpmnParticipantDef(
    val id: String,
    val name: MLText,
    val number: String?,
    val documentation: MLText,
    val processRef: String,
    val ecosType: EntityRef,
    val enabled: Boolean
)
