package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LAST_COMMENT_LOCAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LAST_TASK_COMMENT

@Component
class SetLastTaskCommentTaskListener : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        val lastTaskComment = delegateTask.getVariable(BPMN_LAST_TASK_COMMENT) as String?
        delegateTask.setVariableLocal(BPMN_LAST_COMMENT_LOCAL, lastTaskComment)
    }
}
