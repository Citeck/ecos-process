package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.GROUP_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.splitToUserGroupCandidates
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import javax.annotation.PostConstruct

private const val COLLECTION_START_PREFIX = "["
private const val COLLECTION_END_PREFIX = "]"

private lateinit var utils: UserTaskListenerUtils

@Component
class ManualRecipientsModeUserTaskAssignListener : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        val assignee = delegateTask.assignee
        if (assignee.isNullOrBlank()) {
            return
        }

        val candidatesRaw = assignee.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR)

        log.debug { "candidatesRaw: $candidatesRaw" }

        val candidatesNames = utils.getCandidateAuthorityNames(candidatesRaw)

        delegateTask.assignee = null
        fillTaskRecipients(candidatesNames, delegateTask)
    }

    private fun fillTaskRecipients(candidateNames: List<String>, delegateTask: DelegateTask) {
        val isSingleUser = candidateNames.size == 1 && !candidateNames[0].startsWith(GROUP_PREFIX)
        if (isSingleUser) {
            val assignee = candidateNames[0]

            log.debug { "Set assignee: $assignee" }
            delegateTask.assignee = assignee
        } else {
            candidateNames.forEach {
                if (it.startsWith(GROUP_PREFIX)) {
                    log.debug { "Add candidate group: $it" }
                    delegateTask.addCandidateGroup(it)
                } else {
                    log.debug { "Add candidate user: $it" }
                    delegateTask.addCandidateUser(it)
                }
            }
        }
    }
}

@Component
class RecipientsFromRolesUserTaskAssignListener : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        setUserToAssigneeIfIsSingleCandidateUser(delegateTask)
    }

    private fun setUserToAssigneeIfIsSingleCandidateUser(delegateTask: DelegateTask) {
        if (!delegateTask.assignee.isNullOrBlank()) {
            return
        }

        delegateTask.getSingleUserCandidateIfPossible()?.let { candidate ->
            delegateTask.deleteCandidateUser(candidate)
            delegateTask.assignee = candidate
        }
    }

    private fun DelegateTask.getSingleUserCandidateIfPossible(): String? {
        if (candidates.isEmpty()) {
            return null
        }

        val (candidateUsers, candidateGroups) = candidates.splitToUserGroupCandidates()

        if (candidateGroups.isNotEmpty() || candidateUsers.size != 1) {
            return null
        }

        return candidateUsers.first().ifEmpty {
            null
        }
    }
}

@Component
class MultiInstanceAutoModeUserTaskAssignListener : TaskListener {
    override fun notify(delegateTask: DelegateTask) {
        val value = delegateTask.getVariable(BPMN_ASSIGNEE_ELEMENT) ?: return
        val assignee = value.toString()

        if (assignee.startsWith(GROUP_PREFIX)) {
            delegateTask.addCandidateGroup(assignee)
        } else {
            delegateTask.assignee = assignee
        }
    }
}


@Component
internal class UserTaskListenerUtils(
    val authorityService: EcosAuthoritiesApi
) {

    @PostConstruct
    private fun init() {
        utils = this
    }

    fun getCandidateAuthorityNames(candidates: List<String>): List<String> {
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
