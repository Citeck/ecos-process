package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.getTitle

@Component
class SetTaskTitleTaskListener : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        delegateTask.setVariableLocal(BPMN_NAME_ML, delegateTask.getTitle())
    }
}
