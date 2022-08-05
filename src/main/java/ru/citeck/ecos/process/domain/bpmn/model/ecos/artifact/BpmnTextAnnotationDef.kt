package ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact

import ru.citeck.ecos.commons.data.MLText

data class BpmnTextAnnotationDef(
    val id: String,
    val text: MLText
)
