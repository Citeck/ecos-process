package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow

import ru.citeck.ecos.commons.data.MLText

data class BpmnMessageFlowDef(
    val id: String,
    val name: MLText,
    val sourceRef: String,
    val targetRef: String
)
