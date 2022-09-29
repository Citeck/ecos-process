package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.toTaskEvent
import ru.citeck.ecos.records3.record.request.RequestContext

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnTaskCompleteEventListener(
    private val emitter: BpmnEventEmitter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = delegateTask.toTaskEvent()
                log.debug { "Emit task complete element:\n $converterFlowElement" }

                emitter.emitUserTaskCompleteEvent(converterFlowElement)
            }
        }
    }
}
