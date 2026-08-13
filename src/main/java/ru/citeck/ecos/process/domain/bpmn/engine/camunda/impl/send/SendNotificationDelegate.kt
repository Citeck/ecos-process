package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import net.fortuna.ical4j.model.TimeZone
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.RecipientsSendStrategy
import ru.citeck.ecos.notifications.lib.icalendar.CalendarEvent
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.io.convert.parseRecipientsSendStrategy
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnRecipientsSendStrategy
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.CalendarEventOrganizer
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration
import java.time.Instant
import java.util.*

private const val VAR_NOTIFICATION_ATTACHMENTS = "_attachments"
private const val VAR_EVENT_UID = "eventUid"
private const val VAR_EVENT_SEQUENCE = "eventSequence"
private const val VAR_RECIPIENT_ROLE_ID = "recipientRoleId"
private const val VAR_RECIPIENT_ROLE_NAME = "recipientRoleName"

// Anything outside this set (`:`, `@`, `+`, spaces, ...) is collapsed to '-' in the iCal UID suffix.
private val CALENDAR_KEY_UNSAFE_CHARS = Regex("[^A-Za-z0-9._-]")

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
    var notificationRecipientsStrategy: Expression? = null

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
                EntityRef.valueOf(recordFromExpression.replace("\"", ""))
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

        var lang = notificationLang?.expressionText?.ifBlank { null }
        if (lang == "null") {
            lang = null
        }

        val recipientsSendStrategy = notificationRecipientsStrategy?.expressionText.parseRecipientsSendStrategy()

        val type = notificationType?.let { NotificationType.valueOf(it.expressionText) }
            ?: NotificationType.EMAIL_NOTIFICATION
        val templateRef = EntityRef.valueOf(notificationTemplate?.expressionText)
        val title = notificationTitle?.expressionText ?: ""
        val body = notificationBody?.expressionText ?: ""
        val cc = getRecipientsEmailsFromExpression(notificationCc, execution)
        val bcc = getRecipientsEmailsFromExpression(notificationBcc, execution)

        val messages = buildRecipientMessages(recipientsSendStrategy, execution)

        // Calendar identity and all execute-invariant calendar data (organizer, its timezone,
        // summary/description/date/duration) are resolved once per execute, before any
        // notification is built, so that every split message of this execute reuses the same
        // resolution instead of re-resolving it (and re-querying organizer/timezone) per message.
        val sendCalendarEvent = notificationSendCalendarEvent?.getValue(execution).toString().toBoolean()
        val calendarEventContext = if (sendCalendarEvent) {
            resolveCalendarEventContext(execution)
        } else {
            null
        }

        // The base additional meta (process variables + currentRunAsUser snapshot) is also
        // execute-invariant - resolved once and reused, only the per-message role/attachment bits
        // are added per message.
        val metaFromUserInput = notificationAdditionalMeta?.let {
            Json.mapper.readMap(it.expressionText, String::class.java, Any::class.java)
        } ?: emptyMap()
        val baseAdditionalMeta = getBaseNotificationAdditionalMeta(execution, metaFromUserInput)

        // Notifications (including additionalMeta, which captures the caller's currentRunAsUser)
        // are built with the real caller auth context. Only the actual send is done as system.
        fun buildNotification(message: RecipientsMessage): Notification {
            return Notification.Builder()
                .record(document)
                .notificationType(type)
                .recipientsSendStrategy(RecipientsSendStrategy.COMBINED)
                .title(title)
                .body(body)
                .templateRef(templateRef)
                .recipients(message.recipients)
                .from(notificationFrom)
                .cc(cc)
                .bcc(bcc)
                .lang(lang)
                .additionalMeta(getAdditionalMeta(message, baseAdditionalMeta, calendarEventContext))
                .build()
        }

        val notifications = messages
            .filter { it.recipients.isNotEmpty() }
            .map { buildNotification(it) }
            .ifEmpty {
                // Before per-role/per-recipient splitting this delegate always produced exactly one
                // notification command. Keep that contract: when nothing resolves, still send an
                // empty notification so the notifications app reports RECIPIENTS_NOT_FOUND instead
                // of the send task silently completing without a trace.
                listOf(buildNotification(RecipientsMessage(emptyList(), null, "")))
            }

        // NotificationServiceImpl.send only registers the dispatch via TxnContext.doBeforeCommit,
        // and without an active txn it dispatches immediately. Since a non-COMBINED strategy sends
        // several notifications, a failure midway would otherwise leave the earlier ones already
        // dispatched and a Camunda job retry would duplicate them. One txn makes the batch atomic.
        AuthContext.runAsSystem {
            TxnContext.doInTxn {
                for (notification in notifications) {
                    notificationService.send(notification)
                }
            }
        }
    }

    private data class CalendarEventContext(
        val baseUid: String,
        val sequence: Int,
        val summary: String,
        val description: String,
        val date: Instant,
        val durationInMillis: Long,
        val organizer: String,
        val timeZone: TimeZone
    )

    /**
     * Calendar identity and all execute-invariant calendar data are resolved once per execute:
     * - the uid must stay stable across re-activations (so a re-send is an update of the same
     *   event), and the sequence must be bumped exactly once per activation - not once per split
     *   message.
     * - summary/description/date/duration expressions and the organizer (and its timezone) are
     *   the same for every split message of this execute, so resolving them once here avoids
     *   re-running organizer/timezone resolution (role assignee + email lookups, a person query)
     *   for every message.
     */
    private fun resolveCalendarEventContext(execution: DelegateExecution): CalendarEventContext {
        var baseUid = execution.getVariable(VAR_EVENT_UID)?.toString() ?: ""
        if (baseUid.isBlank()) {
            baseUid = UUID.randomUUID().toString()
            execution.setVariable(VAR_EVENT_UID, baseUid)
        }

        val prevSequence = execution.getVariable(VAR_EVENT_SEQUENCE) as? Int
        val sequence = if (prevSequence == null) {
            0
        } else {
            prevSequence + 1
        }
        execution.setVariable(VAR_EVENT_SEQUENCE, sequence)

        val summary = notificationCalendarEventSummary?.getValue(execution).toString()
        val description = notificationCalendarEventDescription?.getValue(execution).toString()
        val date = notificationCalendarEventDate?.getValue(execution).let {
            if (it is Date) {
                it.toInstant()
            } else {
                Instant.parse(it.toString())
            }
        }
        val duration = Duration.parse(notificationCalendarEventDuration?.getValue(execution).toString())
        val durationInMillis = duration.toMillis()

        val organizer = getCalendarEventOrganizer(notificationCalendarEventOrganizer, execution)
        val organizerTimeZone = mailUtils.getUserTimeZoneByEmail(organizer)

        return CalendarEventContext(
            baseUid = baseUid,
            sequence = sequence,
            summary = summary,
            description = description,
            date = date,
            durationInMillis = durationInMillis,
            organizer = organizer,
            timeZone = CalendarUtils.convertToICalTz(organizerTimeZone)
        )
    }

    /**
     * Parses a recipients expression into role ids and plain expression values, partitioned by
     * [RecipientType]. Expression values are split on [BPMN_CAMUNDA_COLLECTION_SEPARATOR].
     *
     * @return a pair of (role ids, expression values); both empty when [expressionData] is null.
     */
    private fun splitRecipientsByType(
        expressionData: Expression?,
        execution: DelegateExecution
    ): Pair<List<String>, List<String>> {
        val roleIds = mutableListOf<String>()
        val expressionValues = mutableListOf<String>()
        expressionData?.let {
            for (recipient in recipientsFromJson(it.getValue(execution).toString())) {
                when (recipient.type) {
                    RecipientType.ROLE -> roleIds.add(recipient.value)
                    RecipientType.EXPRESSION ->
                        expressionValues.addAll(recipient.value.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR))
                }
            }
        }
        return roleIds to expressionValues
    }

    private fun buildRecipientMessages(
        strategy: BpmnRecipientsSendStrategy,
        execution: DelegateExecution
    ): List<RecipientsMessage> {
        val (roleIds, expressionValues) = splitRecipientsByType(notificationTo, execution)

        return when (strategy) {
            BpmnRecipientsSendStrategy.COMBINED -> {
                val emails = camundaRoleService.getEmails(document, roleIds) + mailUtils.getEmails(expressionValues)
                listOf(RecipientsMessage(emails, null, ""))
            }

            // PER_ROLE and PER_RECIPIENT share the same skeleton (resolve role names once, walk
            // roles then plain expression recipients); they differ only in how one group of emails
            // becomes messages - one message per group vs one message per email.
            BpmnRecipientsSendStrategy.PER_ROLE -> {
                buildRoleMessages(roleIds, expressionValues) { emails, role, keyBase ->
                    listOf(RecipientsMessage(emails, role, keyBase))
                }
            }

            BpmnRecipientsSendStrategy.PER_RECIPIENT -> {
                buildRoleMessages(roleIds, expressionValues) { emails, role, keyBase ->
                    emails.map { email ->
                        val calendarKey = if (keyBase.isEmpty()) email else "$keyBase:$email"
                        RecipientsMessage(listOf(email), role, calendarKey)
                    }
                }
            }
        }
    }

    /**
     * Shared skeleton for the per-role and per-recipient strategies: resolves role display names
     * once, emits messages for each non-empty role group, then for the plain expression recipients.
     * [explode] turns one group of emails into the strategy's messages; its `keyBase` is the role id
     * for role groups and empty for expression recipients.
     */
    private fun buildRoleMessages(
        roleIds: List<String>,
        expressionValues: List<String>,
        explode: (emails: List<String>, role: RecipientRole?, keyBase: String) -> List<RecipientsMessage>
    ): List<RecipientsMessage> {
        val roleNames = camundaRoleService.getRoleNames(document, roleIds)
        val messages = mutableListOf<RecipientsMessage>()
        camundaRoleService.getEmailsByRole(document, roleIds).forEach { (roleId, emails) ->
            if (emails.isNotEmpty()) {
                messages += explode(emails, resolveRole(roleId, roleNames), roleId)
            }
        }
        val expressionEmails = mailUtils.getEmails(expressionValues).toList()
        if (expressionEmails.isNotEmpty()) {
            messages += explode(expressionEmails, null, "")
        }
        return messages
    }

    private fun resolveRole(roleId: String, roleNames: Map<String, MLText>): RecipientRole {
        return RecipientRole(roleId, roleNames[roleId] ?: MLText.EMPTY)
    }

    private data class RecipientsMessage(
        val recipients: List<String>,
        val role: RecipientRole?,
        /**
         * Distinguishes the iCal UID of this message from sibling messages of the same execute.
         */
        val calendarKey: String
    )

    private data class RecipientRole(
        val id: String,
        val name: MLText
    )

    private fun getAdditionalMeta(
        message: RecipientsMessage,
        baseAdditionalMeta: Map<String, Any>,
        calendarEventContext: CalendarEventContext?
    ): Map<String, Any> {
        // Shallow copy: the nested values (notably the "process" map) are shared between the
        // messages of one execute and must stay read-only. Only per-message keys are added here.
        val additionalMeta = baseAdditionalMeta.toMutableMap()

        val role = message.role
        if (role != null) {
            additionalMeta[VAR_RECIPIENT_ROLE_ID] = role.id
            additionalMeta[VAR_RECIPIENT_ROLE_NAME] = role.name
        }

        if (calendarEventContext != null) {
            val eventAttachment = createCalendarEventAttachment(
                message.recipients,
                calendarEventContext,
                message.calendarKey
            )
            additionalMeta[VAR_NOTIFICATION_ATTACHMENTS] = eventAttachment
        }

        return additionalMeta
    }

    private fun createCalendarEventAttachment(
        recipients: List<String>,
        context: CalendarEventContext,
        calendarKey: String
    ): CalendarEvent.CalendarEventAttachment {
        // Sibling messages of one execute carry different attendee lists, so they must not share a
        // UID - calendar clients key on UID and would treat them as conflicting versions of one
        // event. The uid stays stable across re-activations because it derives from the persisted
        // base uid and a deterministic per-message key. The key is sanitized to a conservative
        // charset so role ids / emails never leak `:`, `@` etc. into the UID (and to avoid
        // embedding raw addresses); distinct keys stay distinct after sanitizing.
        val uid = if (calendarKey.isEmpty()) {
            context.baseUid
        } else {
            "${context.baseUid}-${sanitizeCalendarKey(calendarKey)}"
        }

        val calendarEvent = CalendarEvent.Builder(context.summary, context.date)
            .uid(uid)
            .timeZone(context.timeZone)
            .sequence(context.sequence)
            .description(context.description)
            .durationInMillis(context.durationInMillis)
            .organizer(context.organizer)
            .attendees(recipients)
            .build()
        return calendarEvent.createAttachment()
    }

    /**
     * Collapses everything outside `[A-Za-z0-9._-]` to '-' so the per-message key stays a clean,
     * opaque iCal UID suffix (no raw `:`/`@`, no embedded email address). Distinct keys remain
     * distinct: characters are replaced, never dropped.
     */
    private fun sanitizeCalendarKey(calendarKey: String): String {
        return calendarKey.replace(CALENDAR_KEY_UNSAFE_CHARS, "-")
    }

    // Get emails, because at this moment we support only email notifications from BPMN
    private fun getRecipientsEmailsFromExpression(
        expressionData: Expression?,
        execution: DelegateExecution
    ): List<String> {
        if (expressionData == null) {
            return emptyList()
        }

        val (roles, fromExpression) = splitRecipientsByType(expressionData, execution)

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
