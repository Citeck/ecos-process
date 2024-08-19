package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi

private const val COLLECTION_START_PREFIX = "["
private const val COLLECTION_END_PREFIX = "]"

private val log = KotlinLogging.logger {}

@Component
class ManualRecipientsModeUserTaskAssignListener(
    private val userTaskListenerUtils: UserTaskListenerUtils
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        userTaskListenerUtils.convertAssigneeStorageToTaskRecipients(delegateTask)
    }
}

@Component
class RecipientsFromRolesUserTaskAssignListener(
    private val userTaskListenerUtils: UserTaskListenerUtils
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        userTaskListenerUtils.convertAssigneeStorageToTaskRecipients(delegateTask)
    }
}

private fun fillTaskRecipients(candidateNames: List<String>, delegateTask: DelegateTask) {
    val isSingleUser = candidateNames.size == 1 && !candidateNames[0].startsWith(AuthGroup.PREFIX)
    if (isSingleUser) {
        val assignee = candidateNames[0]

        log.debug { "Set assignee: $assignee" }
        delegateTask.assignee = assignee
    } else {
        candidateNames.forEach {
            if (it.startsWith(AuthGroup.PREFIX)) {
                log.debug { "Add candidate group: $it" }
                delegateTask.addCandidateGroup(it)
            } else {
                log.debug { "Add candidate user: $it" }
                delegateTask.addCandidateUser(it)
            }
        }
    }
}

@Component
class MultiInstanceAutoModeUserTaskAssignListener : TaskListener {
    override fun notify(delegateTask: DelegateTask) {
        val value = delegateTask.getVariable(BPMN_ASSIGNEE_ELEMENT) ?: return
        val assignee = value.toString()

        if (assignee.startsWith(AuthGroup.PREFIX)) {
            delegateTask.addCandidateGroup(assignee)
        } else {
            delegateTask.assignee = assignee
        }
    }
}

@Component
class UserTaskListenerUtils(
    val authorityService: EcosAuthoritiesApi
) {

    fun convertAssigneeStorageToTaskRecipients(delegateTask: DelegateTask) {
        val assignee = delegateTask.assignee
        if (assignee.isNullOrBlank()) {
            return
        }

        val candidatesRaw = assignee.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR)

        log.debug { "candidatesRaw: $candidatesRaw" }

        val candidatesNames = getCandidateAuthorityNames(candidatesRaw)

        delegateTask.assignee = null
        fillTaskRecipients(candidatesNames, delegateTask)
    }

    private fun getCandidateAuthorityNames(candidates: List<String>): List<String> {
        return authorityService.getAuthorityNames(
            candidates
                .asSequence()
                .map { it.trim() }
                .map {
                    it.removePrefix(COLLECTION_START_PREFIX)
                }
                .map {
                    it.removeSuffix(COLLECTION_END_PREFIX)
                }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
        )
    }
}
