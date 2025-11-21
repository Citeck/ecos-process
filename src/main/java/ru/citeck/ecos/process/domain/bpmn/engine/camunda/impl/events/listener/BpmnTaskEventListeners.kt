package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnElementConverter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.txn.lib.TxnContext

@Component
class BpmnTaskCreateEventListener : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        // do nothing, stay for backward compatibility
    }
}

@Component
class BpmnTaskAssignEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter,
    private val procUtils: ProcUtils
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            TxnContext.doInTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task assign element:\n $converterFlowElement" }
                procUtils.runAsWsSystemIfRequiredForProcDef(converterFlowElement.procDefId) {
                    emitter.emitUserTaskAssignEvent(converterFlowElement)
                }
            }
        }
    }
}

@Component
class BpmnTaskCompleteEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter,
    private val procUtils: ProcUtils
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            TxnContext.doInTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task complete element:\n $converterFlowElement" }
                procUtils.runAsWsSystemIfRequiredForProcDef(converterFlowElement.procDefId) {
                    emitter.emitUserTaskCompleteEvent(converterFlowElement)
                }
            }
        }
    }
}

@Component
class BpmnTaskDeleteEventListener(
    private val emitter: BpmnEventEmitter,
    private val bpmnElementConverter: BpmnElementConverter,
    private val procUtils: ProcUtils
) : TaskListener {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            TxnContext.doInTxn {
                val converterFlowElement = bpmnElementConverter.toUserTaskEvent(delegateTask)
                log.debug { "Emit task delete element:\n $converterFlowElement" }
                procUtils.runAsWsSystemIfRequiredForProcDef(converterFlowElement.procDefId) {
                    emitter.emitUserTaskDeleteEvent(converterFlowElement)
                }
            }
        }
    }
}
