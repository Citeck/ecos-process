package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.addTaskListener
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_LA_ENABLED
import ru.citeck.ecos.process.domain.bpmn.io.convert.toCamundaKey

@Component
class UserTaskLazyApprovalParseListener(
    private val userTaskLazyApprovalListener: UserTaskLazyApprovalListener
) : AbstractBpmnParseListener() {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl?, activity: ActivityImpl) {
        val isLA = userTaskElement.attribute(BPMN_PROP_LA_ENABLED.toCamundaKey())
            .toBoolean()

        if (isLA) {
            activity.addTaskListener(TaskListener.EVENTNAME_CREATE, userTaskLazyApprovalListener)
            log.trace { "Add userTaskLazyApprovalListener to ${activity.id}" }
        }
    }
}
