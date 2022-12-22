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
    private val addTaskTitleToVariablesListener: AddTaskTitleToVariablesListener,
    private val addTaskPossibleOutcomesToVariablesListener: AddTaskPossibleOutcomesToVariablesListener
) : AbstractBpmnParseListener() {

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, addTaskTitleToVariablesListener)
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, addTaskPossibleOutcomesToVariablesListener)
    }
}
