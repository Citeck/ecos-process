package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.BpmnConditionDef

data class BpmnSequenceFlowDef(
    val id: String,
    val name: MLText,
    val sourceRef: String,
    val targetRef: String,
    val condition: BpmnConditionDef
)
