package ru.citeck.ecos.process.domain.bpmn.model.ecos.pool

import ru.citeck.ecos.commons.data.MLText

data class BpmnLaneDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val flowRefs: List<String>,
    val childLaneSet: BpmnLaneSetDef? = null
)
