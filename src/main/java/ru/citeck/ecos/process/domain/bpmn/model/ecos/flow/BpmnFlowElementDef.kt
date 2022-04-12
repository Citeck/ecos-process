package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow

import ru.citeck.ecos.commons.data.ObjectData

data class BpmnFlowElementDef(
    val id: String,
    val type: String,
    val data: ObjectData
)
