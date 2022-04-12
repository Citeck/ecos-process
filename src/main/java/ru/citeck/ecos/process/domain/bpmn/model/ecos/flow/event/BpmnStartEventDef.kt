package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText

data class BpmnStartEventDef(
    val id: String,
    val name: MLText,
    val outgoing: List<String>
)
