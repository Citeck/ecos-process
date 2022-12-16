package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.app.AppContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert.BpmnDataValue
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val VAR_CURRENT_RUN_AS_USER = "currentRunAsUser"
private const val VAR_PROCESS = "process"

private const val FORCE_STR_PREFIX = "!str_"

private const val PERSON_SOURCE_ID = "person"

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

        // TODO: make document not mandatory? Send to, roles?
        document = execution.getDocumentRef()
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
            .additionalMeta(getAdditionalMeta(execution))
            .build()

        AuthContext.runAsSystem {
            notificationService.send(notification)
        }
    }

    private fun getAdditionalMeta(execution: DelegateExecution): Map<String, Any> {
        val metaFromUserInput = notificationAdditionalMeta?.let {
            Json.mapper.readMap(it.expressionText, String::class.java, Any::class.java)
        } ?: emptyMap()

        val processVariables = execution.getPreparedProcessVariables().toMutableMap()

        processVariables[VAR_CURRENT_RUN_AS_USER] = AuthContext.getCurrentRunAsUserRef()

        val additionalMeta = metaFromUserInput.map { (key, value) ->
            val valueToPut = when (value) {
                is String -> {
                    if (value.startsWith(FORCE_STR_PREFIX)) {
                        value.removePrefix(FORCE_STR_PREFIX)
                    } else {
                        EntityRef.valueOf(value)
                    }
                }

                else -> value
            }
            key to valueToPut
        }.toMap().toMutableMap()

        additionalMeta[VAR_PROCESS] = processVariables

        return additionalMeta
    }

    private fun AuthContext.getCurrentRunAsUserRef(): EntityRef {
        return EntityRef.create(AppName.EMODEL, PERSON_SOURCE_ID, getCurrentRunAsUser())
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

    private fun getRecipientsFromExpression(expressionData: Expression?): List<String> {
        if (expressionData == null) return emptyList()

        val recipients = recipientsFromJson(expressionData.expressionText)

        val isNotRole = { rc: Recipient -> rc.type != RecipientType.ROLE }
        if (recipients.any(isNotRole)) error("Supported only ${RecipientType.ROLE} recipients")

        return AuthContext.runAsSystem { camundaRoleService.getEmails(document, recipients.map { it.value }) }
    }
}
