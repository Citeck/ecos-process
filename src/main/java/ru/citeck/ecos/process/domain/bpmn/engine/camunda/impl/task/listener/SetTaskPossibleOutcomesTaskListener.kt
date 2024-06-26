package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_POSSIBLE_OUTCOMES
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils

@Component
class SetTaskPossibleOutcomesTaskListener(
    private val taskDefinitionUtils: TaskDefinitionUtils
) : TaskListener {
    override fun notify(delegateTask: DelegateTask) {
        delegateTask.setVariableLocal(BPMN_POSSIBLE_OUTCOMES, taskDefinitionUtils.getTaskPossibleOutcomes(delegateTask))
    }
}
