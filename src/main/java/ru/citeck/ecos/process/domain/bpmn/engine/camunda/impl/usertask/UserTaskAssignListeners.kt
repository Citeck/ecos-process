package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnElementConverter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi

private const val COLLECTION_START_PREFIX = "["
private const val COLLECTION_END_PREFIX = "]"

private val log = KotlinLogging.logger {}

@Component
class ManualRecipientsModeUserTaskAssignListener(
    private val utils: UserTaskListenerUtils
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        val converted = utils.convertAssigneeStorageToTaskRecipients(delegateTask)
        utils.sendUserTaskCreateBpmnEvent(converted)
    }
}

@Component
class RecipientsFromRolesUserTaskAssignListener(
    private val utils: UserTaskListenerUtils
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        val converted = utils.convertAssigneeStorageToTaskRecipients(delegateTask)
        utils.sendUserTaskCreateBpmnEvent(converted)
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
    val authorityService: EcosAuthoritiesApi,
    private val emitter: BpmnEventEmitter,

    @Lazy
    private val bpmnElementConverter: BpmnElementConverter
) {

    /**
     * We send [ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_CREATE]
     * in [ManualRecipientsModeUserTaskAssignListener] and [RecipientsFromRolesUserTaskAssignListener] listeners
     * because we need to send event with filled task assignee and candidates.
     */
    fun sendUserTaskCreateBpmnEvent(
        delegateTask: DelegateTask,
    ) {
        AuthContext.runAsSystem {
            TxnContext.doInTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task create element:\n $converterFlowElement" }

                emitter.emitUserTaskCreateEvent(converterFlowElement)
            }
        }
    }

    fun convertAssigneeStorageToTaskRecipients(delegateTask: DelegateTask): DelegateTask {
        val assignee = delegateTask.assignee
        if (assignee.isNullOrBlank()) {
            return delegateTask
        }

        val candidatesRaw = assignee.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR)

        log.debug { "candidatesRaw: $candidatesRaw" }

        val candidatesNames = getCandidateAuthorityNames(candidatesRaw)

        delegateTask.assignee = null
        fillTaskRecipients(candidatesNames, delegateTask)

        return delegateTask
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
