package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.addTaskListener
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_MANUAL_RECIPIENTS_MODE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_MULTI_INSTANCE_AUTO_MODE
import ru.citeck.ecos.process.domain.bpmn.io.convert.toCamundaKey

/**
 * @author Roman Makarskiy
 */
@Component
class UserTaskAssignParseListener(
    private val multiInstanceAutoModeUserTaskAssignListener: MultiInstanceAutoModeUserTaskAssignListener,
    private val manualRecipientsModeUserTaskAssignListener: ManualRecipientsModeUserTaskAssignListener,
    private val recipientsFromRolesUserTaskAssignListener: RecipientsFromRolesUserTaskAssignListener
) : AbstractBpmnParseListener() {

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        val multiInstanceAutoMode = userTaskElement.attribute(BPMN_PROP_MULTI_INSTANCE_AUTO_MODE.toCamundaKey())
            .toBoolean()
        val manualRecipientsMode = userTaskElement.attribute(BPMN_PROP_MANUAL_RECIPIENTS_MODE.toCamundaKey())
            .toBoolean()

        if (multiInstanceAutoMode && manualRecipientsMode) {
            error("UserTask can't be in multi-instance auto mode and manual recipients mode at the same time")
        }

        if (multiInstanceAutoMode) {
            activity.addTaskListener(TaskListener.EVENTNAME_CREATE, multiInstanceAutoModeUserTaskAssignListener)
            return
        }

        if (manualRecipientsMode) {
            activity.addTaskListener(TaskListener.EVENTNAME_CREATE, manualRecipientsModeUserTaskAssignListener)
            return
        }

        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, recipientsFromRolesUserTaskAssignListener)
    }
}
