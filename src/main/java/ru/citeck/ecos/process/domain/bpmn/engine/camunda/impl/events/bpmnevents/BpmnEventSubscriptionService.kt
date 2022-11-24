package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerHandle
import java.util.*
import javax.annotation.PostConstruct

@Service
class BpmnEventSubscriptionService(
    private val camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder,
    private val eventsService: EventsService,
    private val camundaEventProcessor: CamundaEventProcessor
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val listeners = mutableMapOf<String, Pair<CombinedEventSubscription, ListenerHandle>>()

    @PostConstruct
    fun initListeners() {
        val combinedSubscriptions = camundaEventSubscriptionFinder.findAllDeployedSubscriptions().combine()

        for (subscription in combinedSubscriptions) {
            if (listeners.containsKey(subscription.eventName)) {
                throw IllegalStateException("Event ${subscription.eventName} already registered")
            }

            addListener(subscription)
        }

        log.info {
            buildString {
                appendLine("\n================Register BPMN Ecos Event Subscriptions======================")
                for ((eventName, listener) in listeners) {
                    appendLine("Event: $eventName, atts: ${listener.first.attributes}")
                }
                appendLine("============================================================================")
            }
        }
    }

    fun addSubscriptionsForDefRev(procDefRevId: UUID) {
        val subscriptions = camundaEventSubscriptionFinder.getSubscriptionsByProcDefRevId(procDefRevId).combine()

        for (subscription in subscriptions) {
            val (eventName, attributes) = subscription

            if (requireUpdateExistingListener(subscription)) {
                // TODO: At current implementation we just re-registered listener.
                // In future we can just update model - https://citeck.atlassian.net/browse/ECOSENT-2452.

                val currentAtts = listeners[eventName]!!.first.attributes
                val concatenatedAttsSubscription = subscription.copy(attributes = currentAtts + attributes)

                listeners[eventName]?.second?.remove()
                listeners.remove(eventName)

                addListener(concatenatedAttsSubscription)
                log.info {
                    "Update BPMN Ecos Event Subscription Event: $eventName, " +
                        "atts: ${concatenatedAttsSubscription.attributes}"
                }
            } else {
                addListener(subscription)
                log.info { "Register new BPMN Ecos Event Subscription Event: $eventName, atts: $attributes" }
            }
        }
    }

    private fun addListener(subscription: CombinedEventSubscription) {
        val (eventName, attributes) = subscription

        val listener = eventsService.addListener<ObjectData> {
            withDataClass(ObjectData::class.java)
            withEventType(eventName)
            withAttributes(attributes.associateBy { it })
            withAction { event ->
                camundaEventProcessor.processEvent(event)
            }
        }

        listeners[eventName] = subscription to listener
    }

    private fun requireUpdateExistingListener(newSubscription: CombinedEventSubscription): Boolean {
        if (!listeners.containsKey(newSubscription.eventName)) {
            return false
        }

        val existingSubscription = listeners[newSubscription.eventName]!!.first

        return !existingSubscription.attributes.containsAll(newSubscription.attributes)
    }
}
