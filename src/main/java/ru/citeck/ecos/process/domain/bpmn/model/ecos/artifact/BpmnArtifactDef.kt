package ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact

import ru.citeck.ecos.commons.data.ObjectData

data class BpmnArtifactDef(
    val id: String,
    val type: String,
    val data: ObjectData
)
