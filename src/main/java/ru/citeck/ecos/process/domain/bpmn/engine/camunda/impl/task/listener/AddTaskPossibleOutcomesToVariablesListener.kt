package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_POSSIBLE_OUTCOMES
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getPossibleOutcomes

@Component
class AddTaskPossibleOutcomesToVariablesListener : TaskListener {
    override fun notify(delegateTask: DelegateTask) {
        delegateTask.setVariableLocal(VAR_POSSIBLE_OUTCOMES, delegateTask.getPossibleOutcomes())
    }
}
