package ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact

data class BpmnAssociationDef(
    val id: String,
    val sourceRef: String,
    val targetRef: String
)
