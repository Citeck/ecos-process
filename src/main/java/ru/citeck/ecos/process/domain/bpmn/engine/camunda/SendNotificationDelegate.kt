package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName

private const val ALFRESCO_APP = "alfresco"
private const val AUTHORITY_SRC_ID = "authority"
private const val PEOPLE_SRC_ID = "people"
private const val WORKSPACE_PREFIX = "workspace://"

class SendNotificationDelegate : JavaDelegate {

    var notificationTemplate: Expression? = null
    var notificationRecord: Expression? = null
    var notificationTitle: Expression? = null
    var notificationBody: Expression? = null

    var notificationTo: Expression? = null
    var notificationCc: Expression? = null
    var notificationBcc: Expression? = null

    var notificationType: Expression? = null

    var notificationLang: Expression? = null
    var notificationAdditionalMeta: Expression? = null

    private lateinit var notificationService: NotificationService
    private lateinit var roleService: RoleService
    private lateinit var recordsService: RecordsService
    private lateinit var document: RecordRef

    private fun init(execution: DelegateExecution) {
        notificationService = AppContext.getBean(NotificationService::class.java)
        roleService = AppContext.getBean(RoleService::class.java)
        recordsService = AppContext.getBean(RecordsService::class.java)

        document = let {
            val documentFromVar = execution.getDocument()
            if (documentFromVar == RecordRef.EMPTY) error("Document is mandatory variable")
            documentFromVar
        }
    }

    override fun execute(execution: DelegateExecution) {
        init(execution)

        val record = let {
            val recordFromExpression = notificationRecord?.expressionText ?: ""
            if (recordFromExpression.isNotBlank()) {
                RecordRef.valueOf(recordFromExpression)
            } else {
                document
            }
        }

        val notification = Notification.Builder()
            .record(record)
            .notificationType(notificationType?.let { NotificationType.valueOf(it.expressionText) }
                ?: NotificationType.EMAIL_NOTIFICATION
            )
            .title(notificationTitle?.expressionText ?: "")
            .body(notificationBody?.expressionText ?: "")
            .templateRef(RecordRef.valueOf(notificationTemplate?.expressionText))
            .recipients(getRecipientsFromExpression(notificationTo))
            .cc(getRecipientsFromExpression(notificationCc))
            .bcc(getRecipientsFromExpression(notificationBcc))
            .lang(notificationLang?.expressionText)
            .additionalMeta(notificationAdditionalMeta?.let {
                Json.mapper.readMap(it.expressionText, String::class.java, String::class.java)
            } ?: emptyMap())
            .build()

        AuthContext.runAsSystem {
            notificationService.send(notification)
        }
    }

    private fun getRecipientsFromExpression(expressionData: Expression?): List<String> {
        if (expressionData == null) return emptyList()

        val recipients = recipientsFromJson(expressionData.expressionText)

        val isNotRole = { rc: Recipient -> rc.type != RecipientType.ROLE }
        if (recipients.any(isNotRole)) error("Supported only ${RecipientType.ROLE} recipients")

        return AuthContext.runAsSystem { getEmailsFromRoles(recipients.map { it.value }) }
    }

    private fun getEmailsFromRoles(roles: List<String>): List<String> {
        val recipientNames = roles.map {
            roleService.getAssignees(document, it)
        }.flatten().toSet()
        val recipientsFullFilledRefs = convertRecipientsToFullFilledRefs(recipientNames)

        val allUsers = mutableSetOf<RecordRef>()
        val groups = mutableListOf<RecordRef>()

        recipientsFullFilledRefs.forEach {
            if (it.isAuthorityGroupRef()) groups.add(it) else allUsers.add(it)
        }

        val usersFromGroup = recordsService.getAtts(groups, GroupInfo::class.java).map { it.containedUsers }.flatten()
        allUsers.addAll(usersFromGroup)

        return recordsService.getAtts(allUsers, UserInfo::class.java)
            .filter { it.email?.isNotBlank() ?: false }
            .map { it.email!! }
    }

    /**
     * Role service return userName or groupName. We need convert it to full recordRef format.
     * TODO: migrate to model (microservice) people/groups after completion of development.
     */
    private fun convertRecipientsToFullFilledRefs(recipients: Collection<String>): List<RecordRef> {
        return recipients.map {
            if (it.startsWith(WORKSPACE_PREFIX)) {
                throw IllegalArgumentException("NodeRef format does not support. Recipient: $it")
            }

            var fullFilledRef = RecordRef.valueOf(it)

            if (fullFilledRef.appName.isBlank()) {
                fullFilledRef = fullFilledRef.addAppName(ALFRESCO_APP)
            }

            if (fullFilledRef.sourceId.isBlank()) {
                val sourceId = if (fullFilledRef.isAuthorityGroupRef()) AUTHORITY_SRC_ID else PEOPLE_SRC_ID
                fullFilledRef = fullFilledRef.withSourceId(sourceId)
            }

            fullFilledRef
        }
    }
}

private data class GroupInfo(
    @AttName("containedUsers")
    val containedUsers: List<RecordRef> = emptyList()
)

private data class UserInfo(
    @AttName("email")
    var email: String? = "",
)
