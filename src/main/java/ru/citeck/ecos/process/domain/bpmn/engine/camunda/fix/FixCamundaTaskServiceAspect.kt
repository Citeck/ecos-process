package ru.citeck.ecos.process.domain.bpmn.engine.camunda.fix

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.interceptor.CommandContext
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.camunda.bpm.engine.impl.persistence.entity.IdentityLinkEntity
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.impl.util.EnsureUtil
import org.camunda.bpm.engine.task.IdentityLink
import org.camunda.bpm.engine.task.IdentityLinkType
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.stream.Collectors

/**
 * This aspect is used to fix the https://jira.camunda.com/browse/CAM-8494 issue.
 * TODO: remove after camunda 7.18.0 migration (fixed in 7.18.0)
 */
@Aspect
@Component
class FixCamundaTaskServiceAspect(
    private val camundaCommandExecutor: CommandExecutor
) {

    @Around(
        value = "execution(* org.camunda.bpm.engine.impl.TaskServiceImpl.getIdentityLinksForTask(..))",
        argNames = "joinPoint"
    )
    fun getIdentityLinksForTask(joinPoint: ProceedingJoinPoint): Any? {
        val taskId = joinPoint.args[0] as String
        val getIdentityLinkCmd = FixedGetIdentityLinksForTaskCmd(taskId)
        return camundaCommandExecutor.execute(getIdentityLinkCmd)
    }
}

/**
 * This class is a copy of the original GetIdentityLinksForTaskCmd class from the Camunda engine and is used to fix the
 * https://jira.camunda.com/browse/CAM-8494 issue.
 * TODO: remove after camunda 7.18.0 migration (fixed in 7.18.0)
 */
class FixedGetIdentityLinksForTaskCmd(
    private val taskId: String
) : Command<MutableList<IdentityLink>>, Serializable {

    override fun execute(commandContext: CommandContext): MutableList<IdentityLink> {
        EnsureUtil.ensureNotNull("taskId", taskId)

        val taskManager = commandContext.taskManager
        val task = taskManager.findTaskById(taskId)
        EnsureUtil.ensureNotNull("Cannot find task with id $taskId", "task", task)

        checkGetIdentityLink(task, commandContext)

        val identityLinks = task.identityLinks.stream().collect(Collectors.toList())

        // assignee is not part of identity links in the db.
        // so if there is one, we add it here.
        // @Tom: we discussed this long on skype and you agreed ;-)
        // an assignee *is* an identityLink, and so must it be reflected in the API
        //
        // Note: we cant move this code to the TaskEntity (which would be cleaner),
        // since the task.delete cascased to all associated identityLinks
        // and of course this leads to exception while trying to delete a non-existing identityLink
        if (task.assignee != null) {
            val identityLink = IdentityLinkEntity()
            identityLink.userId = task.assignee
            identityLink.task = task
            identityLink.type = IdentityLinkType.ASSIGNEE
            identityLinks.add(identityLink)
        }
        if (task.owner != null) {
            val identityLink = IdentityLinkEntity()
            identityLink.userId = task.owner
            identityLink.task = task
            identityLink.type = IdentityLinkType.OWNER
            identityLinks.add(identityLink)
        }

        return identityLinks.toMutableList()
    }

    private fun checkGetIdentityLink(task: TaskEntity?, commandContext: CommandContext) {
        for (checker in commandContext.processEngineConfiguration.commandCheckers) {
            checker.checkReadTask(task)
        }
    }
}
