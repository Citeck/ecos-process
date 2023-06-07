package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener

import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.addTaskListener

@Component
class TaskVariablesParseListener(
    private val setTaskTitleTaskListener: SetTaskTitleTaskListener,
    private val setTaskPossibleOutcomesTaskListener: SetTaskPossibleOutcomesTaskListener,
    private val setTaskSenderTaskListener: SetTaskSenderTaskListener,
    private val setLastTaskCommentTaskListener: SetLastTaskCommentTaskListener
) : AbstractBpmnParseListener() {

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, setTaskTitleTaskListener)
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, setTaskPossibleOutcomesTaskListener)
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, setTaskSenderTaskListener)
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, setLastTaskCommentTaskListener)
    }
}
