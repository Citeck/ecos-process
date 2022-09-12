package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.GROUP_PREFIX
import ru.citeck.ecos.webapp.api.authority.EcosAuthorityService

private const val COLLECTION_START_PREFIX = "["
private const val COLLECTION_END_PREFIX = "]"

/**
 * @author Roman Makarskiy
 */
@Component
class ManualRecipientsModeUserTaskAssignListener(
    private val authorityService: EcosAuthorityService
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        val assignee = delegateTask.assignee
        if (assignee.isNullOrBlank()) {
            return
        }

        val candidatesRaw = assignee.split(CAMUNDA_COLLECTION_SEPARATOR)

        log.debug { "candidatesRaw: $candidatesRaw" }

        val candidatesNames = getCandidateAuthorityNames(candidatesRaw)

        delegateTask.assignee = null
        fillCandidates(candidatesNames, delegateTask)
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

    private fun fillCandidates(candidateNames: List<String>, delegateTask: DelegateTask) {
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
