package ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram

import ru.citeck.ecos.commons.data.ObjectData

data class BpmnDiagramElementDef(
    val id: String,
    val type: String,
    val data: ObjectData
)
