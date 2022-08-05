package ru.citeck.ecos.process.domain.bpmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class BpmnDefinitionDef(
    val id: String,

    val enabled: Boolean,
    val autoStartEnabled: Boolean,

    val definitionsId: String,

    val name: MLText,
    val ecosType: EntityRef,
    val formRef: EntityRef,
    val sectionRef: EntityRef,

    // TODO: single or list?
    val process: BpmnProcessDef,

    val diagrams: List<BpmnDiagramDef>,

    val exporter: String,
    val exporterVersion: String,
    val targetNamespace: String
)
