package ru.citeck.ecos.process.domain.bpmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef

class BpmnProcessDef(
    val id: String,
    val definitionsId: String,

    val name: MLText,
    val ecosType: RecordRef
)
