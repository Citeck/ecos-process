package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.records2.RecordRef
import java.util.*

data class BpmnSendTaskDef(
    val id: String,
    val name: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val record: RecordRef = RecordRef.EMPTY,
    val template: RecordRef = RecordRef.EMPTY,
    val type: NotificationType,

    val from: String = "",

    val to: List<Recipient> = emptyList(),
    val cc: List<Recipient> = emptyList(),
    val bcc: List<Recipient> = emptyList(),

    val title: String = "",
    val body: String = "",

    val lang: Locale? = null,

    val additionalMeta: Map<String, Any> = emptyMap(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig
) {

    init {
        if (body.isBlank() && RecordRef.isEmpty(template)) {
            throw EcosBpmnElementDefinitionException(id, "Template is mandatory parameter with empty body")
        }

        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) {
            throw EcosBpmnElementDefinitionException(id, "All recipients is empty")
        }

        if (type != NotificationType.EMAIL_NOTIFICATION) {
            throw EcosBpmnElementDefinitionException(id, "In the current version, only the email type is supported")
        }
    }
}
