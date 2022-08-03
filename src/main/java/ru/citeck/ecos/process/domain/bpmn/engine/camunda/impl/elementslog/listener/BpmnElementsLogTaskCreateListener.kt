package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.BpmnElementsLogEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.toTaskElement
import ru.citeck.ecos.records3.record.request.RequestContext

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementsLogTaskCreateListener(
    private val emitter: BpmnElementsLogEmitter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = delegateTask.toTaskElement()
                log.debug { "Emit task create element:\n $converterFlowElement" }

                emitter.emitTaskCreateEvent(converterFlowElement)
            }
        }
    }
}
