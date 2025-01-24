package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.icalendar.CalendarEvent
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.CalendarEventOrganizer
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration
import java.time.Instant
import java.util.*

private const val VAR_NOTIFICATION_ATTACHMENTS = "_attachments"
private const val VAR_EVENT_UID = "eventUid"
private const val VAR_EVENT_SEQUENCE = "eventSequence"

private const val PERSON_SOURCE_ID = "person"

class SendNotificationDelegate : JavaDelegate {

    var notificationTemplate: Expression? = null
    var notificationRecord: Expression? = null
    var notificationTitle: Expression? = null
    var notificationBody: Expression? = null

    var notificationFrom: Expression? = null

    var notificationTo: Expression? = null
    var notificationCc: Expression? = null
    var notificationBcc: Expression? = null

    var notificationType: Expression? = null

    var notificationLang: Expression? = null
    var notificationAdditionalMeta: Expression? = null

    var notificationSendCalendarEvent: Expression? = null
    var notificationCalendarEventOrganizer: Expression? = null
    var notificationCalendarEventSummary: Expression? = null
    var notificationCalendarEventDescription: Expression? = null
    var notificationCalendarEventDate: Expression? = null
    var notificationCalendarEventDuration: Expression? = null

    private lateinit var notificationService: NotificationService
    private lateinit var camundaRoleService: CamundaRoleService
    private lateinit var mailUtils: MailUtils
    private lateinit var document: EntityRef

    private fun init(execution: DelegateExecution) {
        notificationService = AppContext.getBean(NotificationService::class.java)
        camundaRoleService = AppContext.getBean(CamundaRoleService::class.java)
        mailUtils = AppContext.getBean(MailUtils::class.java)
        document = execution.getDocumentRef()
    }

    override fun execute(execution: DelegateExecution) {
        init(execution)

        document = let {
            val recordFromExpression = notificationRecord?.getValue(execution)?.toString()
            if (!recordFromExpression.isNullOrBlank()) {
                EntityRef.valueOf(recordFromExpression)
            } else {
                execution.getDocumentRef()
            }
        }

        val notificationFrom = let {
            val evaluatedFrom = notificationFrom?.getValue(execution)?.toString()
            if (evaluatedFrom.isNullOrBlank()) {
                null
            } else {
                evaluatedFrom
            }
        }

        val recipients = getRecipientsEmailsFromExpression(notificationTo, execution)
        val notification = Notification.Builder()
            .record(document)
            .notificationType(
                notificationType?.let { NotificationType.valueOf(it.expressionText) }
                    ?: NotificationType.EMAIL_NOTIFICATION
            )
            .title(notificationTitle?.expressionText ?: "")
            .body(notificationBody?.expressionText ?: "")
            .templateRef(EntityRef.valueOf(notificationTemplate?.expressionText))
            .recipients(recipients)
            .from(notificationFrom)
            .cc(getRecipientsEmailsFromExpression(notificationCc, execution))
            .bcc(getRecipientsEmailsFromExpression(notificationBcc, execution))
            .lang(notificationLang?.expressionText)
            .additionalMeta(getAdditionalMeta(recipients, execution))
            .build()

        AuthContext.runAsSystem {
            notificationService.send(notification)
        }
    }

    private fun getAdditionalMeta(recipients: List<String>, execution: DelegateExecution): Map<String, Any> {
        val metaFromUserInput = notificationAdditionalMeta?.let {
            Json.mapper.readMap(it.expressionText, String::class.java, Any::class.java)
        } ?: emptyMap()

        val additionalMeta = getBaseNotificationAdditionalMeta(execution, metaFromUserInput).toMutableMap()

        val sendCalendarEvent = notificationSendCalendarEvent?.getValue(execution).toString().toBoolean()
        if (sendCalendarEvent) {
            val eventAttachment = createCalendarEventAttachment(recipients, execution)
            additionalMeta[VAR_NOTIFICATION_ATTACHMENTS] = eventAttachment
        }

        return additionalMeta
    }

    private fun createCalendarEventAttachment(
        recipients: List<String>,
        execution: DelegateExecution
    ): CalendarEvent.CalendarEventAttachment {
        val eventSummary = notificationCalendarEventSummary?.getValue(execution).toString()
        val eventDescription = notificationCalendarEventDescription?.getValue(execution).toString()
        val eventDate = notificationCalendarEventDate?.getValue(execution).let {
            if (it is Date) {
                it.toInstant()
            } else {
                Instant.parse(it.toString())
            }
        }
        val eventDuration = Duration.parse(notificationCalendarEventDuration?.getValue(execution).toString())
        val eventDurationInMillis = eventDuration.toMillis()

        val eventOrganizer = getCalendarEventOrganizer(notificationCalendarEventOrganizer, execution)

        val organizerTimeZone =
            mailUtils.getUserTimeZoneByEmail(eventOrganizer)

        var uid = execution.getVariable(VAR_EVENT_UID)?.toString() ?: ""
        if (uid.isBlank()) {
            uid = UUID.randomUUID().toString()
            execution.setVariable(VAR_EVENT_UID, uid)
        }

        var sequence = execution.getVariable(VAR_EVENT_SEQUENCE) as? Int
        if (sequence == null) {
            sequence = 0
        } else {
            sequence++
        }
        execution.setVariable(VAR_EVENT_SEQUENCE, sequence)

        val calendarEvent = CalendarEvent.Builder(eventSummary, eventDate)
            .uid(uid)
            .timeZone(CalendarUtils.convertToICalTz(organizerTimeZone))
            .sequence(sequence)
            .description(eventDescription)
            .durationInMillis(eventDurationInMillis)
            .organizer(eventOrganizer)
            .attendees(recipients)
            .build()
        return calendarEvent.createAttachment()
    }

    private fun AuthContext.getCurrentRunAsUserRef(): EntityRef {
        val user = getCurrentRunAsUser()
        if (user.isBlank()) {
            return EntityRef.EMPTY
        }
        return EntityRef.create(AppName.EMODEL, PERSON_SOURCE_ID, user)
    }

    private fun DelegateExecution.getPreparedProcessVariables(): Map<String, Any> {
        return variables.map { (key, value) ->
            val valueToPut = when (value) {
                is BpmnDataValue -> value.asDataValue()
                else -> value
            }
            key to valueToPut
        }.toMap()
    }

    // Get emails, because at this moment we support only email notifications from BPMN
    private fun getRecipientsEmailsFromExpression(
        expressionData: Expression?,
        execution: DelegateExecution
    ): List<String> {
        if (expressionData == null) {
            return emptyList()
        }

        val roles = mutableListOf<String>()
        val fromExpression = mutableListOf<String>()

        val recipients = recipientsFromJson(expressionData.getValue(execution).toString())
        for (recipient in recipients) {
            when (recipient.type) {
                RecipientType.ROLE -> {
                    roles.add(recipient.value)
                }

                RecipientType.EXPRESSION -> {
                    fromExpression.addAll(recipient.value.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR))
                }
            }
        }

        val emailsFromRoles = camundaRoleService.getEmails(document, roles)
        val emailsFromExpression = mailUtils.getEmails(fromExpression)

        return emailsFromRoles + emailsFromExpression
    }

    private fun getCalendarEventOrganizer(expressionData: Expression?, execution: DelegateExecution): String {
        if (expressionData == null) {
            return ""
        }

        val calendarEventOrganizer = Json.mapper.read(
            expressionData.getValue(execution).toString(),
            CalendarEventOrganizer::class.java
        ) ?: return ""

        val emailFromRole = camundaRoleService.getEmails(document, listOf(calendarEventOrganizer.role))
        val emailFromExpression = mailUtils.getEmails(listOf(calendarEventOrganizer.expression))

        return if (emailFromRole.isNotEmpty()) {
            emailFromRole.first()
        } else if (emailFromExpression.isNotEmpty()) {
            emailFromExpression.first()
        } else {
            ""
        }
    }
}
