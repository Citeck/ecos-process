package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

data class BpmnSendTaskDef(
    val id: String,
    val name: MLText,
    val number: Int?,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val record: EntityRef = EntityRef.EMPTY,
    val template: EntityRef = EntityRef.EMPTY,
    val type: NotificationType,

    val from: String = "",

    val to: List<Recipient> = emptyList(),
    val cc: List<Recipient> = emptyList(),
    val bcc: List<Recipient> = emptyList(),

    val title: String = "",
    val body: String = "",

    val lang: Locale? = null,

    val additionalMeta: Map<String, Any> = emptyMap(),

    val sendCalendarEvent: Boolean = false,
    val calendarEventOrganizer: String = "",
    val calendarEventSummary: String = "",
    val calendarEventDescription: String = "",
    val calendarEventDate: String = "",
    val calendarEventDuration: String = "",

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig
) : Validated {

    override fun validate() {
        if (body.isBlank() && EntityRef.isEmpty(template)) {
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
