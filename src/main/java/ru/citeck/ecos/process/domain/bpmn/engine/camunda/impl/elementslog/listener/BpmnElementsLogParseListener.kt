package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.listener

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
class BpmnElementsLogParseListener(
    private val bpmnLogExecutionListener: BpmnElementsLogExecutionListener,
    private val bpmnTaskCreateListener: BpmnElementsLogTaskCreateListener,
    private val bpmnTaskCompleteListener: BpmnElementsLogTaskCompleteListener
) : AbstractBpmnParseListener() {

    private fun addActivityFlowElementListener(activity: ActivityImpl) {
        activity.addBuiltInListener(ExecutionListener.EVENTNAME_START, bpmnLogExecutionListener)
    }

    private fun addSequenceFlowElementListener(activity: TransitionImpl) {
        activity.addBuiltInListener(ExecutionListener.EVENTNAME_TAKE, bpmnLogExecutionListener)
    }

    private fun addUserTaskListeners(activity: ActivityImpl) {
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, bpmnTaskCreateListener)
        activity.addTaskListener(TaskListener.EVENTNAME_COMPLETE, bpmnTaskCompleteListener)
    }

    override fun parseStartEvent(startEventElement: Element, scope: ScopeImpl, startEventActivity: ActivityImpl) {
        addActivityFlowElementListener(startEventActivity)
    }

    override fun parseExclusiveGateway(exclusiveGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseInclusiveGateway(inclusiveGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseParallelGateway(parallelGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseScriptTask(scriptTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseServiceTask(serviceTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseTask(taskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseManualTask(manualTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addUserTaskListeners(activity)
    }

    override fun parseEndEvent(endEventElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseSequenceFlow(sequenceFlowElement: Element, scopeElement: ScopeImpl, transition: TransitionImpl) {
        addSequenceFlowElementListener(transition)
    }

    override fun parseSendTask(sendTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseReceiveTask(receiveTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseEventBasedGateway(eventBasedGwElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseIntermediateThrowEvent(
        intermediateEventElement: Element,
        scope: ScopeImpl,
        activity: ActivityImpl
    ) {
        super.parseIntermediateThrowEvent(intermediateEventElement, scope, activity)
    }

    override fun parseIntermediateCatchEvent(
        intermediateEventElement: Element,
        scope: ScopeImpl,
        activity: ActivityImpl
    ) {
        super.parseIntermediateCatchEvent(intermediateEventElement, scope, activity)
    }

    override fun parseBoundaryEvent(
        boundaryEventElement: Element,
        scopeElement: ScopeImpl,
        nestedActivity: ActivityImpl
    ) {
        super.parseBoundaryEvent(boundaryEventElement, scopeElement, nestedActivity)
    }

    override fun parseSubProcess(subProcessElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }

    override fun parseCallActivity(callActivityElement: Element?, scope: ScopeImpl?, activity: ActivityImpl) {
        addActivityFlowElementListener(activity)
    }
}
