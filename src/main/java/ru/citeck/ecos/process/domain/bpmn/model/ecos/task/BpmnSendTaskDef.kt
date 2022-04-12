package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.records2.RecordRef

data class BpmnSendTaskDef(
    val id: String,
    val name: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val record: RecordRef = RecordRef.EMPTY,
    val template: RecordRef = RecordRef.EMPTY,

    val title: String = "",
    val body: String = ""
) {

    init {
        if (body.isBlank() && RecordRef.isEmpty(template)) {
            throw EcosBpmnDefinitionException("Template is mandatory parameter with empty body")
        }
    }

}
