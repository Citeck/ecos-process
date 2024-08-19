package ru.citeck.ecos.process.domain.bpmn.process

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.txn.lib.TxnContext

@Component
class BpmnProcessAsyncStarter(
    private val bpmnProcessService: BpmnProcessService,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.count}")
    private val consumersCount: Int,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.prefetch}")
    private val prefetch: Int,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.retry.max-attempts}")
    private val maxAttempts: Int,
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        log.info { "Starting BPMN process async starter with $consumersCount consumers" }

        for (i in 1..consumersCount) {
            bpmnRabbitmqConnection.doWithNewChannel(prefetch, false) { channel ->
                channel.addConsumerWithRetrying(
                    BPMN_ASYNC_START_PROCESS_QUEUE_NAME,
                    StartProcessRequest::class.java,
                    maxAttempts,
                ) { request, _ ->
                    onMessageReceived(request.getContent(), "consumer-$i")
                }
            }
        }
    }

    fun onMessageReceived(msg: StartProcessRequest, tag: String? = null) {
        log.debug {
            "Received start process request with tag $tag, request: $msg"
        }

        val workflowInitiator = msg.variables[BPMN_WORKFLOW_INITIATOR]?.toString()

        TxnContext.doInNewTxn {
            if (workflowInitiator.isNullOrBlank()) {
                AuthContext.runAsSystem {
                    bpmnProcessService.startProcess(msg)
                }
            } else {
                AuthContext.runAsFull(workflowInitiator) {
                    bpmnProcessService.startProcess(msg)
                }
            }
        }
    }
}
