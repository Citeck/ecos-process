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
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.*
import ru.citeck.ecos.txn.lib.TxnContext

private const val BPMN_ELEMENT_BEFORE_COMMIT_ORDER = 1000.0f

private fun sendBpmnElementBeforeCommit(unit: () -> Unit) {
    TxnContext.doBeforeCommit(BPMN_ELEMENT_BEFORE_COMMIT_ORDER) {
        AuthContext.runAsSystem {
            unit.invoke()
        }
    }
}

@Component
class BpmnFlowElementTakeEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?,
    private val bpmnElementConverter: BpmnElementConverter
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

        val rawFlowElement = bpmnElementConverter.toRawFlowElement(execution)
        log.trace { "Send raw bpmn element take to queue:\n $rawFlowElement" }

        sendBpmnElementBeforeCommit {
            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_FLOW_ELEMENT_TAKE)
            )
        }
    }
}

@Component
class BpmnActivityStartEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?,
    private val bpmnElementConverter: BpmnElementConverter
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

        val rawFlowElement = bpmnElementConverter.toRawFlowElement(execution)
        log.trace { "Send raw bpmn element start to queue:\n $rawFlowElement" }

        sendBpmnElementBeforeCommit {
            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_ACTIVITY_ELEMENT_START)
            )
        }
    }
}

@Component
class BpmnActivityEndEventExecutionListener(
    @Autowired(required = false)
    private val bpmnElementsToQueuePublisher: BpmnElementsToQueuePublisher?,
    private val bpmnElementConverter: BpmnElementConverter
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

        val rawFlowElement = bpmnElementConverter.toRawFlowElement(execution)
        log.trace { "Send raw bpmn element end to queue:\n $rawFlowElement" }

        sendBpmnElementBeforeCommit {
            bpmnElementsToQueuePublisher.sendToQueue(
                BpmnElementProcessingRequest(DataValue.of(rawFlowElement), BPMN_EVENT_ACTIVITY_ELEMENT_END)
            )
        }
    }
}
