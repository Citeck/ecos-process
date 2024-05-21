package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.elements.BpmnElementProcessingRequest
import ru.citeck.ecos.process.domain.bpmn.elements.BpmnElementsToQueuePublisher
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_ACTIVITY_ELEMENT_END
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_ACTIVITY_ELEMENT_START
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_FLOW_ELEMENT_TAKE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.toRawFlowElement

@Component
class BpmnFlowElementTakeEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?
) : ExecutionListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(execution: DelegateExecution) {
        if (bpmnElementsToQueuePublisher == null) {
            return
        }

        val flowElement = execution.bpmnModelElementInstance
        if (flowElement == null || flowElement.id.isNullOrBlank()) {
            return
        }

        AuthContext.runAsSystem {
            val rawFlowElement = execution.toRawFlowElement()

            log.trace { "Send raw bpmn element take to queue:\n $rawFlowElement" }

            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_FLOW_ELEMENT_TAKE)
            )
        }
    }
}

@Component
class BpmnActivityStartEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?
) : ExecutionListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(execution: DelegateExecution) {
        if (bpmnElementsToQueuePublisher == null) {
            return
        }

        val flowElement = execution.bpmnModelElementInstance
        if (flowElement == null || flowElement.id.isNullOrBlank()) {
            return
        }

        AuthContext.runAsSystem {
            val rawFlowElement = execution.toRawFlowElement()
            log.trace { "Send raw bpmn element start to queue:\n $rawFlowElement" }

            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_ACTIVITY_ELEMENT_START)
            )
        }
    }
}

@Component
class BpmnActivityEndEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?
) : ExecutionListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(execution: DelegateExecution) {
        if (bpmnElementsToQueuePublisher == null) {
            return
        }

        val flowElement = execution.bpmnModelElementInstance
        if (flowElement == null || flowElement.id.isNullOrBlank()) {
            return
        }

        AuthContext.runAsSystem {
            val rawFlowElement = execution.toRawFlowElement()
            log.trace { "Send raw bpmn element end to queue:\n $rawFlowElement" }

            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_ACTIVITY_ELEMENT_END)
            )
        }
    }
}
