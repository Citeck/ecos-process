package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.GROUP_PREFIX

/**
 * @author Roman Makarskiy
 */
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
