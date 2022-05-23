package ru.citeck.ecos.process.domain.bpmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.records2.RecordRef

data class BpmnDefinitionDef(
    val id: String,

    val enabled: Boolean,
    val autoStartEnabled: Boolean,

    val definitionsId: String,

    val name: MLText,
    val ecosType: RecordRef,
    val formRef: RecordRef,

    // TODO: single or list?
    val process: BpmnProcessDef,

    val diagrams: List<BpmnDiagramDef>,

    val exporter: String,
    val exporterVersion: String,
    val targetNamespace: String
)
