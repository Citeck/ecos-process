package ru.citeck.ecos.process.domain.bpmn.process

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.BpmnEventSubscriptionService
import ru.citeck.ecos.process.domain.bpmn.process.delayed.BpmnDelayedStartService
import ru.citeck.ecos.rabbitmq.RabbitMqChannel
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection

@Component
class BpmnProcessAsyncStarter(
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection,
    private val delayedStartService: BpmnDelayedStartService,
    private val eventSubscriptionService: BpmnEventSubscriptionService,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.count}")
    private val consumersCount: Int,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.prefetch}")
    private val prefetch: Int,

    @Value("\${ecos-process.bpmn.async-start-process.consumer.retry.max-attempts}")
    private val maxAttempts: Int,
) {
    companion object {
        private val log = KotlinLogging.logger {}
        private const val DLQ_NAME = BPMN_ASYNC_START_PROCESS_QUEUE_NAME + RabbitMqChannel.DLQ_POSTFIX
        private const val EVENT_INIT_TIMEOUT_MS = 10 * 60 * 1000L
    }

    @Volatile
    private var disposed = false

    @PreDestroy
    fun dispose() {
        disposed = true
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

        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            channel.addAckedConsumer(
                DLQ_NAME,
                StartProcessRequest::class.java,
            ) { msg, _ ->
                try {
                    val content = msg.getContent()
                    log.info { "Received DLQ message, saving for delayed retry: $content" }
                    delayedStartService.saveForDelayedRetry(content)
                } catch (e: Throwable) {
                    log.error(e) { "Failed to save DLQ message for delayed retry" }
                    for (i in 1..10) {
                        if (disposed) break
                        Thread.sleep(1_000)
                    }
                    msg.nack()
                }
            }
        }
    }

    fun onMessageReceived(msg: StartProcessRequest, tag: String? = null) {
        awaitEventSubscriptionsInitialized()

        log.debug { "Received start process request with tag $tag, request: $msg" }

        delayedStartService.startProcessWithAuth(msg)
    }

    private fun awaitEventSubscriptionsInitialized() {
        if (eventSubscriptionService.isInitialized()) {
            return
        }
        log.info { "Waiting for BPMN event subscriptions to be initialized..." }
        val deadline = System.currentTimeMillis() + EVENT_INIT_TIMEOUT_MS
        while (!eventSubscriptionService.isInitialized()) {
            if (disposed) {
                log.info { "Disposed, aborting wait for event subscriptions initialization" }
                return
            }
            if (System.currentTimeMillis() > deadline) {
                log.error { "Timed out waiting for BPMN event subscriptions initialization" }
                break
            }
            Thread.sleep(1_000)
        }
        log.info { "BPMN event subscriptions initialized, proceeding" }
    }
}
