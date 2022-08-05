package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.BpmnElementsLogEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.toFlowElement
import ru.citeck.ecos.records3.record.request.RequestContext

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementsLogExecutionListener(
    private val emitter: BpmnElementsLogEmitter
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
