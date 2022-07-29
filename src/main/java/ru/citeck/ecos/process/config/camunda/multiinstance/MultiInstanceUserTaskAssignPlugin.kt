package ru.citeck.ecos.process.config.camunda.multiinstance

import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants
import org.springframework.stereotype.Component

private const val MULTI_INSTANCE_CONFIG_TAG_NAME = "multiInstanceLoopCharacteristics"

/**
 * @author Roman Makarskiy
 */
@Component
class MultiInstanceUserTaskAssignPlugin(
    private val multiInstanceUserTaskAssignListener: MultiInstanceUserTaskAssignListener
) : AbstractBpmnParseListener() {

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        val multiInstanceConfig = userTaskElement.element(MULTI_INSTANCE_CONFIG_TAG_NAME) ?: return

        if (isMultiInstanceAutoMode(multiInstanceConfig)) {
            addListenerToSetTaskAssigneesAutomatically(activity)
        }
    }

    private fun isMultiInstanceAutoMode(multiInstanceConfig: Element): Boolean {
        val collection = multiInstanceConfig.attributeNS(
            BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS,
            BpmnModelConstants.CAMUNDA_ATTRIBUTE_COLLECTION
        ) ?: ""

        return collection.startsWith("\${roles.getAuthorityNames")
    }

    private fun addListenerToSetTaskAssigneesAutomatically(activity: ActivityImpl) {
        val activityBehavior = activity.activityBehavior
        if (activityBehavior is UserTaskActivityBehavior) {
            activityBehavior.taskDefinition.addTaskListener(
                TaskListener.EVENTNAME_CREATE,
                multiInstanceUserTaskAssignListener
            )
        }
    }
}
