package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.records2.RecordRef

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
    private lateinit var camundaRoleService: CamundaRoleService
    private lateinit var document: RecordRef

    private fun init(execution: DelegateExecution) {
        notificationService = AppContext.getBean(NotificationService::class.java)
        camundaRoleService = AppContext.getBean(CamundaRoleService::class.java)

        document = let {
            val documentFromVar = execution.getDocument()
            if (RecordRef.isEmpty(documentFromVar)) error("Document is mandatory variable")
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
            .notificationType(
                notificationType?.let { NotificationType.valueOf(it.expressionText) }
                    ?: NotificationType.EMAIL_NOTIFICATION
            )
            .title(notificationTitle?.expressionText ?: "")
            .body(notificationBody?.expressionText ?: "")
            .templateRef(RecordRef.valueOf(notificationTemplate?.expressionText))
            .recipients(getRecipientsFromExpression(notificationTo))
            .cc(getRecipientsFromExpression(notificationCc))
            .bcc(getRecipientsFromExpression(notificationBcc))
            .lang(notificationLang?.expressionText)
            .additionalMeta(
                notificationAdditionalMeta?.let {
                    Json.mapper.readMap(it.expressionText, String::class.java, String::class.java)
                } ?: emptyMap()
            )
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

        return AuthContext.runAsSystem { camundaRoleService.getEmails(document, recipients.map { it.value }) }
    }
}
