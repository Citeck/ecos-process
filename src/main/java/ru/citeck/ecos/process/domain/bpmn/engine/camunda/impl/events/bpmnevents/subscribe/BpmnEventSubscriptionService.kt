package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerHandle
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventProcessor
import java.time.Instant
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

    private val listeners = mutableMapOf<String, Pair<CombinedEventSubscription, List<ListenerHandle>>>()

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

            if (listeners.containsKey(eventName)) {
                if (requireUpdateExistingListener(subscription)) {
                    log.debug {
                        "BPMN Ecos Event Subscription Event: $eventName exists and need to update model"
                    }

                    // TODO: At current implementation we just re-registered listener.
                    // In future we can just update model - https://citeck.atlassian.net/browse/ECOSENT-2452.

                    val currentAtts = listeners[eventName]!!.first.attributes
                    val concatenatedAttsSubscription = subscription.copy(attributes = currentAtts + attributes)

                    listeners[eventName]?.second?.forEach { it.remove() }
                    listeners.remove(eventName)

                    addListener(concatenatedAttsSubscription)
                } else {
                    log.debug {
                        "BPMN Ecos Event Subscription Event: $eventName exists and no need to update"
                    }
                }
            } else {
                log.debug { "Register new BPMN Ecos Event Subscription Event: $eventName" }
                addListener(subscription)
            }
        }
    }

    private fun addListener(subscription: CombinedEventSubscription) {
        val (eventName, attributes) = subscription

        val listenerEventNames = let {
            val eventType = EcosEventType.from(eventName)
            if (eventType == EcosEventType.UNDEFINED) {
                listOf(eventName)
            } else {
                eventType.availableEventNames()
            }
        }

        log.debug {
            "BPMN Ecos Events Subscription <$eventName> add listeners to EventsService with " +
                "eventTypes: $listenerEventNames, atts: $attributes"
        }

        val addedListeners = listenerEventNames.map { listenerEventName ->
            eventsService.addListener<ObjectData> {
                withDataClass(ObjectData::class.java)
                withEventType(listenerEventName)
                withAttributes(attributes.associateBy { it })
                withAction { event ->
                    log.debug { "Receive subscription event: $event" }
                    camundaEventProcessor.processEvent(event.toGeneralEvent())
                }
            }
        }

        listeners[eventName] = subscription to addedListeners
    }

    private fun requireUpdateExistingListener(newSubscription: CombinedEventSubscription): Boolean {
        val existingSubscription = listeners[newSubscription.eventName]!!.first
        return !existingSubscription.attributes.containsAll(newSubscription.attributes)
    }
}

private fun ObjectData.toGeneralEvent(): GeneralEvent {

    lateinit var id: String
    lateinit var time: Instant
    lateinit var type: String
    lateinit var user: String
    val attributes = mutableMapOf<String, DataValue>()

    this.forEach { att, dataValue ->
        when (att) {
            "\$event.id" -> id = dataValue.asText()
            "\$event.time" -> time = dataValue.getAsInstant()!!
            "\$event.type" -> type = dataValue.asText()
            "\$event.user" -> user = dataValue.asText()
            else -> attributes[att] = dataValue
        }
    }

    require(id.isNotBlank()) { "Event id is blank" }
    require(type.isNotBlank()) { "Event type is blank" }
    require(user.isNotBlank()) { "Event user is blank" }

    return GeneralEvent(id, time, type, user, attributes)
}

data class GeneralEvent(
    val id: String,
    val time: Instant,
    val type: String,
    val user: String,
    val attributes: Map<String, DataValue>
)
