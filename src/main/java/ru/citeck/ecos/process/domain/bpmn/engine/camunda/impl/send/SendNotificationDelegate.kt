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
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert.BpmnDataValue
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
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

    var notificationFrom: Expression? = null

    var notificationTo: Expression? = null
    var notificationCc: Expression? = null
    var notificationBcc: Expression? = null

    var notificationType: Expression? = null

    var notificationLang: Expression? = null
    var notificationAdditionalMeta: Expression? = null

    private lateinit var notificationService: NotificationService
    private lateinit var camundaRoleService: CamundaRoleService
    private lateinit var mailUtils: MailUtils
    private lateinit var document: RecordRef

    private fun init(execution: DelegateExecution) {
        notificationService = AppContext.getBean(NotificationService::class.java)
        camundaRoleService = AppContext.getBean(CamundaRoleService::class.java)
        mailUtils = AppContext.getBean(MailUtils::class.java)
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

        val notificationFrom = let {
            val evaluatedFrom = notificationFrom?.getValue(execution)?.toString()
            if (evaluatedFrom.isNullOrBlank()) {
                null
            } else {
                evaluatedFrom
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
            .recipients(getRecipientsEmailsFromExpression(notificationTo, execution))
            .from(notificationFrom)
            .cc(getRecipientsEmailsFromExpression(notificationCc, execution))
            .bcc(getRecipientsEmailsFromExpression(notificationBcc, execution))
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
                    fromExpression.addAll(recipient.value.split(CAMUNDA_COLLECTION_SEPARATOR))
                }
            }
        }

        val emailsFromRoles = AuthContext.runAsSystem {
            camundaRoleService.getEmails(document, roles)
        }

        val emailsFromExpression = AuthContext.run {
            mailUtils.getEmails(fromExpression)
        }

        return emailsFromRoles + emailsFromExpression
    }
}
