package ru.citeck.ecos.process.domain.bpmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnCollaborationDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.signal.BpmnSignalDef
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

    val process: List<BpmnProcessDef>,
    val collaboration: BpmnCollaborationDef?,

    val signals: List<BpmnSignalDef>,
    /*val messages: List<BpmnMessageDef>,*/

    val diagrams: List<BpmnDiagramDef>,

    val exporter: String,
    val exporterVersion: String,
    val targetNamespace: String
)
