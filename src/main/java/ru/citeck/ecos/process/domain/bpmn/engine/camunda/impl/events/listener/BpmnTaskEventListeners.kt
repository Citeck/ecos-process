package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnElementConverter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.records3.record.request.RequestContext

@Component
class BpmnTaskCreateEventListener(
    private val emitter: BpmnEventEmitter,

    @Lazy
    private val bpmnElementConverter: BpmnElementConverter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task create element:\n $converterFlowElement" }

                emitter.emitUserTaskCreateEvent(converterFlowElement)
            }
        }
    }
}

@Component
class BpmnTaskAssignEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task assign element:\n $converterFlowElement" }

                emitter.emitUserTaskAssignEvent(converterFlowElement)
            }
        }
    }
}

@Component
class BpmnTaskCompleteEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task complete element:\n $converterFlowElement" }

                emitter.emitUserTaskCompleteEvent(converterFlowElement)
            }
        }
    }
}

@Component
class BpmnTaskDeleteEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task delete element:\n $converterFlowElement" }

                emitter.emitUserTaskDeleteEvent(converterFlowElement)
            }
        }
    }
}
