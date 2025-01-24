package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.utils.StringUtils
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

    val record: String = "",
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
    val calendarEventSummary: String = "",
    val calendarEventDescription: String = "",
    val calendarEventOrganizer: CalendarEventOrganizer,
    val calendarEventDate: String = "",
    val calendarEventDateExpression: String = "",
    val calendarEventDuration: String = "",
    val calendarEventDurationExpression: String = "",

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

        if (sendCalendarEvent) {
            if (StringUtils.isBlank(calendarEventSummary)) {
                throw EcosBpmnElementDefinitionException(id, "Calendar event Summary is empty")
            }

            if (StringUtils.isBlank(calendarEventDescription)) {
                throw EcosBpmnElementDefinitionException(id, "Calendar event Description is empty")
            }

            if (calendarEventOrganizer.isEmpty()) {
                throw EcosBpmnElementDefinitionException(id, "Calendar event Organizer is empty")
            }

            if (StringUtils.isBlank(calendarEventDate) && StringUtils.isBlank(calendarEventDateExpression)) {
                throw EcosBpmnElementDefinitionException(id, "Calendar event Date is empty")
            }

            if (StringUtils.isBlank(calendarEventDuration) && StringUtils.isBlank(calendarEventDurationExpression)) {
                throw EcosBpmnElementDefinitionException(id, "Calendar event Duration is empty")
            }
        }
    }
}
