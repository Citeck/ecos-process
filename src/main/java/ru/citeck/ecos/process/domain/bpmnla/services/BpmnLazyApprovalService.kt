package ru.citeck.ecos.process.domain.bpmnla.services

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.license.EcosLicense
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getDocumentRef
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.MailUtils
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessService
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import java.util.*

@Component
class BpmnLazyApprovalService(
    @Lazy
    private val bpmnProcessService: BpmnProcessService,
    @Lazy
    private val procTaskService: ProcTaskService,
    private val procDefService: ProcDefService,
    private val ecosConfigService: EcosConfigService,
    private val notificationService: NotificationService,
    private val mailUtils: MailUtils
) {

    private val isEnt = EcosLicense.getForWebApp { it.isEnterprise() }

    companion object {
        private val log = KotlinLogging.logger {}
        private val DEFAULT_COMMENT_KEY = "lazy-approval-default-comment"
        private val MAIL_FOR_ANSWER_KEY = "lazy-approval-mail-for-reply"
    }

    fun sendNotification(delegateTask: DelegateTask) {

//        if (!isEnt()) {
//            log.info("The lazy approval functionality is only available for the enterprise version.")
//            return
//        }

        val candidates = if (delegateTask.assignee != null) {
            listOf(delegateTask.assignee)
        } else {
            delegateTask.candidates.map { it.userId ?: it.groupId }
        }
        val mailsOfRecipients = mailUtils.getEmails(candidates)
        if (mailsOfRecipients.isEmpty()) {
            log.debug {
                "The message was not sent because the recipient's email address is missing. " +
                    "Task = ${delegateTask.taskDefinitionKey}"
            }
            return
        }

        val processDefinition = bpmnProcessService.getProcessDefinition(delegateTask.processDefinitionId)
        val deploymentId = processDefinition?.deploymentId
        if (deploymentId == null) {
            log.debug { "DeploymentId is null for delegateTask = ${delegateTask.id}" }
            return
        }

        val taskDefinition = getBpmnUserTasksDefByDeploymentId(deploymentId)
            .find { it.id == delegateTask.taskDefinitionKey }
        if (taskDefinition == null) {
            log.debug { "Task definition not found for task = ${delegateTask.taskDefinitionKey}" }
        }

        when (taskDefinition?.notificationType) {
            NotificationType.EMAIL_NOTIFICATION.toString() -> {
                val templateRef = getBpmnUserTasksDefByDeploymentId(deploymentId)
                    .find { it.id == delegateTask.taskDefinitionKey }?.notificationTemplate
                if (templateRef == null) {
                    log.debug { "Notification template is null for task = ${delegateTask.taskDefinitionKey}" }
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
                log.debug { "Unknown notification type : ${taskDefinition?.notificationType}" }
            }
        }
    }

    fun addTokenLAToTask(delegateTask: DelegateTask): UUID {
        val token = UUID.randomUUID()
        delegateTask.setVariableLocal("tokenLA", token)
        return token
    }

    fun getAdditionalMeta(taskId: String, token: UUID): Map<String, Any> {
        val meta = mutableMapOf<String, Any>()
        meta["task_id"] = taskId
        meta["task_token"] = token.toString()
        meta["default_comment"] = ecosConfigService.getValue(DEFAULT_COMMENT_KEY).asText()
        meta["mail_for_answer"] = ecosConfigService.getValue(MAIL_FOR_ANSWER_KEY).asText()

        return meta
    }

    fun approveTask(taskId: String, taskOutcome: String, userId: String, token: String, comment: String?) {
        val task = procTaskService.getTaskById(taskId)
        if (task == null) {
            log.debug { "Task with id = ${taskId} not found!" }
            return
        }

        val outcome = task.possibleOutcomes.find { it.id == taskOutcome }
        if (outcome == null) {
            log.debug { "Task with id = ${taskId} has no outcome with id = ${taskOutcome}!" }
            return
        }

        val tokenLA = procTaskService.getVariableLocal(taskId, "tokenLA").toString()
        if (token != tokenLA) {
            log.debug { "Task with id = ${taskId} has no token = ${token}! tokenLA = ${tokenLA}" }
            return
        }

        val taskOutcome = task.definitionKey?.let { Outcome(it, outcome.id, outcome.name) }
        if (taskOutcome == null) {
            log.debug { "Task with id = ${taskId} has no outcome = ${outcome}!" }
            return
        }

        AuthContext.runAsFull(userId) {
            procTaskService.completeTask(CompleteTaskData(
                task = task,
                outcome = taskOutcome,
                variables = comment?.let { mapOf("comment" to it) } ?: emptyMap()
            ))
        }
    }

    @Cacheable(cacheNames = [BPMN_USER_TASKS_DEF_BY_DEPLOYMENT_ID_CACHE_NAME])
    fun getBpmnUserTasksDefByDeploymentId(deploymentId: String): List<BpmnUserTaskDef> {
        val defRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)
        if (defRev == null) {
            log.error { "Process definition revision is null. DeploymentId: $deploymentId" }
            return emptyList()
        }

        return defRev.getBpmnUserTasksDef()
    }
}

const val BPMN_USER_TASKS_DEF_BY_DEPLOYMENT_ID_CACHE_NAME = "bpmn-user-tasks-def-by-deployment-id-cache"

fun ProcDefRevDto.getBpmnUserTasksDef(): List<BpmnUserTaskDef> {
    val defXml = String(this.data, Charsets.UTF_8)

    val resultTaskDefList = arrayListOf<BpmnUserTaskDef>()

    BpmnIO.importEcosBpmn(defXml).process.forEach { process ->
        resultTaskDefList.addAll(
            process.flowElements
                .filter { it.type == "bpmn:BpmnUserTask" }
                .mapNotNull { it.data.getAs(BpmnUserTaskDef::class.java) }
        )
    }

    return resultTaskDefList
}
