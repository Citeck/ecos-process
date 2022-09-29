package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.toFlowElement
import ru.citeck.ecos.records3.record.request.RequestContext

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnFlowElementEventExecutionListener(
    private val emitter: BpmnEventEmitter
) : ExecutionListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(execution: DelegateExecution) {
        val flowElement = execution.bpmnModelElementInstance
        if (flowElement == null || flowElement.id.isNullOrBlank()) {
            return
        }

        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = execution.toFlowElement()
                log.debug { "Emit flow element:\n $converterFlowElement" }

                emitter.emitElementStart(converterFlowElement)
            }
        }
    }
}
