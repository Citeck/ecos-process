package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LAST_TASK_COMPLETOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_SENDER
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR

@Component
class SetTaskSenderTaskListener : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        val sender = let {
            val lastTaskCompletor = delegateTask.getVariable(BPMN_LAST_TASK_COMPLETOR) as String? ?: ""
            lastTaskCompletor.ifBlank {
                delegateTask.execution.getVariable(BPMN_WORKFLOW_INITIATOR) as String? ?: ""
            }
        }

        if (sender.isNotBlank()) {
            delegateTask.setVariableLocal(BPMN_TASK_SENDER, sender)
        }
    }
}
