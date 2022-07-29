package ru.citeck.ecos.process.config.camunda.multiinstance

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.GROUP_PREFIX

/**
 * @author Roman Makarskiy
 */
@Component
class MultiInstanceUserTaskAssignListener : TaskListener {
    override fun notify(delegateTask: DelegateTask) {
        val value = delegateTask.getVariable(VAR_ASSIGNEE_ELEMENT) ?: return
        val assignee = value.toString()

        if (assignee.startsWith(GROUP_PREFIX)) {
            delegateTask.addCandidateGroup(assignee)
        } else {
            delegateTask.assignee = assignee
        }
    }
}
