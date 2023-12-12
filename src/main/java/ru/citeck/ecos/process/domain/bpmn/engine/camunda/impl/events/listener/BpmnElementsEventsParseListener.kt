package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.pvm.process.TransitionImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.addTaskListener

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementsEventsParseListener(
    private val bpmnActivityStartEventExecutionListener: BpmnActivityStartEventExecutionListener,
    private val bpmnActivityEndEventExecutionListener: BpmnActivityEndEventExecutionListener,
    private val bpmnFlowElementTakeEventExecutionListener: BpmnFlowElementTakeEventExecutionListener,
    private val bpmnTaskCreateListener: BpmnTaskCreateEventListener,
    private val bpmnTaskCompleteListener: BpmnTaskCompleteEventListener,
    private val bpmnTaskAssignListener: BpmnTaskAssignEventListener
) : AbstractBpmnParseListener() {

    private fun addActivityListeners(activity: ActivityImpl) {
        activity.addBuiltInListener(ExecutionListener.EVENTNAME_START, bpmnActivityStartEventExecutionListener)
        activity.addBuiltInListener(ExecutionListener.EVENTNAME_END, bpmnActivityEndEventExecutionListener)
    }

    private fun addSequenceFlowElementListener(activity: TransitionImpl) {
        activity.addBuiltInListener(ExecutionListener.EVENTNAME_TAKE, bpmnFlowElementTakeEventExecutionListener)
    }

    private fun addUserTaskListeners(activity: ActivityImpl) {
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, bpmnTaskCreateListener)
        activity.addTaskListener(TaskListener.EVENTNAME_COMPLETE, bpmnTaskCompleteListener)
        activity.addTaskListener(TaskListener.EVENTNAME_ASSIGNMENT, bpmnTaskAssignListener)
    }

    override fun parseStartEvent(startEventElement: Element, scope: ScopeImpl, startEventActivity: ActivityImpl) {
        addActivityListeners(startEventActivity)
    }

    override fun parseExclusiveGateway(exclusiveGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseInclusiveGateway(inclusiveGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseParallelGateway(parallelGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseScriptTask(scriptTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseServiceTask(serviceTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseTask(taskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseManualTask(manualTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        // addActivityListeners(activity)
        addUserTaskListeners(activity)
    }

    override fun parseEndEvent(endEventElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseSequenceFlow(sequenceFlowElement: Element, scopeElement: ScopeImpl, transition: TransitionImpl) {
        addSequenceFlowElementListener(transition)
    }

    override fun parseSendTask(sendTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseReceiveTask(receiveTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseEventBasedGateway(eventBasedGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseIntermediateThrowEvent(
        intermediateEventElement: Element,
        scope: ScopeImpl,
        activity: ActivityImpl
    ) {
        addActivityListeners(activity)
    }

    override fun parseIntermediateCatchEvent(
        intermediateEventElement: Element,
        scope: ScopeImpl,
        activity: ActivityImpl
    ) {
        addActivityListeners(activity)
    }

    override fun parseBoundaryEvent(
        boundaryEventElement: Element,
        scopeElement: ScopeImpl,
        nestedActivity: ActivityImpl
    ) {
        addActivityListeners(nestedActivity)
    }

    override fun parseSubProcess(subProcessElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseCallActivity(callActivityElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseBusinessRuleTask(businessRuleTaskElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityListeners(activity)
    }

    override fun parseTransaction(transactionElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityListeners(activity)
    }
}
