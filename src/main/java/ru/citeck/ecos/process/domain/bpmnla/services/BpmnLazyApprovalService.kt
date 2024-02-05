package ru.citeck.ecos.process.domain.bpmnla.services

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.lazyapproval.api.BpmnLazyApprovalRemoteApi
import ru.citeck.ecos.license.EcosLicense
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_COMMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.toTaskEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import java.util.*

@Component
class BpmnLazyApprovalService(
    @Lazy
    private val procTaskService: ProcTaskService,
    private val ecosConfigService: EcosConfigService,
    private val notificationService: NotificationService,
    private val mailUtils: MailUtils
) : BpmnLazyApprovalRemoteApi {

    private val hasLicense = EcosLicense.getForEntLib {
        it.get("developer").asBoolean() || it.get("features").has("lazy-approval")
    }

    companion object {
        private val log = KotlinLogging.logger {}
        private const val DEFAULT_COMMENT_KEY = "lazy-approval-default-comment"
        private const val MAIL_FOR_ANSWER_KEY = "lazy-approval-mail-for-reply"
        private const val TASK_TOKEN_NAME = "tokenLA"
    }

    fun sendNotification(delegateTask: DelegateTask) {

        if (!hasLicense()) {
            log.warn("The lazy approval functionality is only available for the enterprise version.")
            return
        }

        val candidates = if (delegateTask.assignee != null) {
            listOf(delegateTask.assignee)
        } else {
            delegateTask.candidates.flatMap { listOfNotNull(it.userId, it.groupId) }
        }
        val mailsOfRecipients = mailUtils.getEmails(candidates)
        if (mailsOfRecipients.isEmpty()) {
            log.debug {
                "The message was not sent because the recipient's email address is missing. " +
                    "Task = ${delegateTask.taskDefinitionKey}"
            }
            return
        }

        val taskEvent = delegateTask.toTaskEvent()

        when (taskEvent.laNotificationType) {
            NotificationType.EMAIL_NOTIFICATION -> {
                val templateRef = taskEvent.laNotificationTemplate
                if (templateRef == null) {
                    log.warn { "Notification template is null for task = ${delegateTask.taskDefinitionKey}" }
                    return
                }

                val tokenLA = addTokenLAToTask(delegateTask)
                val additionalMeta = getAdditionalMeta(delegateTask.id, tokenLA)

                val notification = Notification.Builder()
                    .record(delegateTask.getDocumentRef())
                    .notificationType(NotificationType.EMAIL_NOTIFICATION)
                    .templateRef(templateRef)
                    .recipients(mailsOfRecipients)
                    .additionalMeta(additionalMeta)
                    .build()

                notificationService.send(notification)
                log.debug("Send lazy approval notification! Notification: {}", notification)
            }

            else -> {
                log.warn { "Unknown notification type : ${taskEvent.laNotificationType}" }
            }
        }
    }

    private fun addTokenLAToTask(delegateTask: DelegateTask): UUID {
        val token = UUID.randomUUID()
        delegateTask.setVariableLocal(TASK_TOKEN_NAME, token)
        return token
    }

    private fun getAdditionalMeta(taskId: String, token: UUID): Map<String, Any> {
        val meta = mutableMapOf<String, Any>()
        meta["task_id"] = taskId
        meta["task_token"] = token.toString()
        meta["default_comment"] = ecosConfigService.getValue(DEFAULT_COMMENT_KEY).asText()
        meta["mail_for_answer"] = ecosConfigService.getValue(MAIL_FOR_ANSWER_KEY).asText()

        return meta
    }

    override fun approveTask(taskId: String, taskOutcome: String, userId: String, token: String, comment: String) {
        val task = procTaskService.getTaskById(taskId)
        if (task == null) {
            log.warn { "Task with id = ${taskId} not found!" }
            return
        }

        val outcome = task.possibleOutcomes.find { it.id == taskOutcome }
        if (outcome == null) {
            log.warn { "Task with id = ${taskId} has no outcome with id = ${taskOutcome}!" }
            return
        }

        val tokenLA = procTaskService.getVariableLocal(taskId, TASK_TOKEN_NAME).toString()
        if (token != tokenLA) {
            log.warn { "Task with id = ${taskId} has no token = ${token}! tokenLA = ${tokenLA}" }
            return
        }

        val completeTaskOutcome = task.definitionKey?.let { Outcome(it, outcome.id, outcome.name) }
        if (completeTaskOutcome == null) {
            log.warn { "Task with id = ${taskId} has no outcome = ${outcome}!" }
            return
        }

        AuthContext.runAsFull(userId) {
            procTaskService.completeTask(CompleteTaskData(
                task = task,
                outcome = completeTaskOutcome,
                variables = mapOf(BPMN_COMMENT to comment)
            ))
        }
    }
}
