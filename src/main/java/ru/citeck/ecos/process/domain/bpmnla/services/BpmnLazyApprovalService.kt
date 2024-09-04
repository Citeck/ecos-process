package ru.citeck.ecos.process.domain.bpmnla.services

import mu.KotlinLogging
import org.apache.commons.lang3.exception.ExceptionUtils
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.lazyapproval.api.BpmnLazyApprovalRemoteApi
import ru.citeck.ecos.lazyapproval.dto.LazyApprovalReportDto
import ru.citeck.ecos.lazyapproval.model.MailProcessingCode
import ru.citeck.ecos.license.EcosLicense
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_COMMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnElementConverter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

private const val FORCE_STR_PREFIX = "!str_"
@Component
class BpmnLazyApprovalService(
    @Lazy
    private val procTaskService: ProcTaskService,
    @Lazy
    private val procHistoricTaskService: ProcHistoricTaskService,
    @Lazy
    private val bpmnProcessService: BpmnProcessService,
    private val ecosConfigService: EcosConfigService,
    private val notificationService: NotificationService,
    private val mailUtils: MailUtils,
    private val recordsService: RecordsService,

    @Lazy
    private val bpmnElementConverter: BpmnElementConverter,
    @Lazy
    private val taskDefinitionUtils: TaskDefinitionUtils
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

        val taskEvent = bpmnElementConverter.toUserTaskEvent(delegateTask)

        fun getTemplateRef(): EntityRef? {
            if (!taskEvent.laManualNotificationTemplateEnabled) {
                return taskEvent.laNotificationTemplate
            }

            val manualNotificationTemplate = taskEvent.laManualNotificationTemplate
            if (manualNotificationTemplate.isNullOrBlank()) {
                return null
            }

            val executionVariable = findExecutionVariable(manualNotificationTemplate)
            return EntityRef.valueOf(
                if (executionVariable != null) {
                    delegateTask.execution.getVariable(executionVariable)
                } else {
                    manualNotificationTemplate
                }
            )
        }

        when (taskEvent.laNotificationType) {
            NotificationType.EMAIL_NOTIFICATION -> {
                val templateRef = getTemplateRef()
                if (templateRef == null) {
                    log.warn { "Notification template is null for task = ${delegateTask.taskDefinitionKey}" }
                    return
                }

                val tokenLA = addTokenLAToTask(delegateTask)
                val additionalMeta = getAdditionalMeta(delegateTask, tokenLA, taskEvent.laNotificationAdditionalMeta)

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

    private fun getAdditionalMeta(
        delegateTask: DelegateTask,
        token: UUID,
        additionalMeta: Map<String, Any>
    ): Map<String, Any> {
        val meta = mutableMapOf<String, Any>()
        meta["task_id"] = delegateTask.id
        meta["task_token"] = token.toString()
        meta["default_comment"] = ecosConfigService.getValue(DEFAULT_COMMENT_KEY).asText()
        meta["mail_for_answer"] = ecosConfigService.getValue(MAIL_FOR_ANSWER_KEY).asText()
        meta["process"] = delegateTask.execution.variables

        additionalMeta.forEach { (key, value) ->
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
            meta[key] = valueToPut
        }
        return meta
    }

    override fun approveTask(
        taskId: String,
        taskOutcome: String,
        userId: String,
        token: String,
        comment: String
    ): LazyApprovalReportDto {
        val task = procTaskService.getTaskById(taskId)

        if (task == null) {
            procHistoricTaskService.getHistoricTaskById(taskId)?.let {
                return if (it.ended != null) {
                    log.warn { "Task with id = $taskId already completed!" }
                    fillLazyApprovalReport(MailProcessingCode.TASK_ALREADY_COMPLETED, it)
                } else {
                    log.warn { "Task with id = $taskId is inactive but not completed!" }
                    fillLazyApprovalReport(MailProcessingCode.EXCEPTION, it)
                }
            }

            log.warn { "Task with id = $taskId not found!" }
            return fillLazyApprovalReport(MailProcessingCode.TASK_NOT_FOUND, null)
        }

        val outcome = task.possibleOutcomes.find { it.id == taskOutcome }
        if (outcome == null) {
            log.warn { "Task with id = $taskId has no outcome with id = $taskOutcome!" }
            return fillLazyApprovalReport(MailProcessingCode.OUTCOME_NOT_FOUND, task)
        }

        val tokenLA = procTaskService.getVariableLocal(taskId, TASK_TOKEN_NAME).toString()
        if (token != tokenLA) {
            log.warn { "Task with id = $taskId has no token = $token! tokenLA = $tokenLA" }
            return fillLazyApprovalReport(MailProcessingCode.TOKEN_NOT_FOUND, task)
        }

        val outcomeForTask = Outcome.OUTCOME_PREFIX + taskOutcome
        val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, taskId))
        recordAtt.setAtt(outcomeForTask, true)
        recordAtt.setAtt(BPMN_COMMENT, comment)

        return try {
            TxnContext.doInNewTxn {
                AuthContext.runAsFull(userId) {
                    recordsService.mutate(recordAtt)
                }

                fillLazyApprovalReport(MailProcessingCode.OK, task)
            }
        } catch (e: Exception) {
            val message = (ExceptionUtils.getRootCause(e) ?: e).message ?: "empty"
            log.warn(e) { "Task with id = $taskId failed! The following error occurred: $message" }
            fillLazyApprovalReport(MailProcessingCode.EXCEPTION, message, task)
        }
    }

    private fun fillLazyApprovalReport(code: MailProcessingCode, task: ProcTaskDto?): LazyApprovalReportDto {
        return fillLazyApprovalReport(code, null, task)
    }

    private fun fillLazyApprovalReport(
        code: MailProcessingCode,
        errorMessage: String?,
        task: ProcTaskDto?
    ): LazyApprovalReportDto {

        val report = LazyApprovalReportDto(
            processingCode = code,
            errorMessage = errorMessage,
            documentRef = task?.documentRef,
            taskName = task?.name
        )

        val taskDefinitionKey = task?.definitionKey
        val processDefinitionId = task?.processInstanceId?.getLocalId()?.let {
            bpmnProcessService.getProcessDefinitionByProcessInstanceId(it)?.id
        }
        if (taskDefinitionKey?.isNotBlank() == true && processDefinitionId?.isNotBlank() == true) {
            val taskInfo = taskDefinitionUtils.getUserTaskLaInfo(processDefinitionId to taskDefinitionKey)
            report.laReportEnabled = taskInfo.laReportEnabled
            report.laSuccessReportNotificationTemplate = taskInfo.laSuccessReportNotificationTemplate ?: EntityRef.EMPTY
            report.laErrorReportNotificationTemplate = taskInfo.laErrorReportNotificationTemplate ?: EntityRef.EMPTY
        }

        return report
    }

    private fun findExecutionVariable(input: String): String? {
        val regex = Regex("^\\$\\{(.*)}$")
        val matchResult = regex.find(input)
        return matchResult?.groupValues?.get(1)
    }
}
