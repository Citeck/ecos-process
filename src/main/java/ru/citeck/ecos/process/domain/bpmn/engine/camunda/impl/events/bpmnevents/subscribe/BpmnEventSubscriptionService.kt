package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerHandle
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.domain.bpmn.elements.config.BPMN_PROCESS_ELEMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional.BpmnConditionalEventsProcessor
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional.RecordUpdatedEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventProcessor
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.thread

@Service
class BpmnEventSubscriptionService(
    private val webappApi: EcosWebAppApi,
    private val camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder,
    private val eventsService: EventsService,
    private val camundaEventProcessor: CamundaEventProcessor,
    private val bpmnConditionalEventsProcessor: BpmnConditionalEventsProcessor,
    private val ecosTypesRegistry: EcosTypesRegistry
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val eventListeners =
        Collections.synchronizedMap(mutableMapOf<String, Pair<CombinedEventSubscription, List<ListenerHandle>>>())
    private val conditionalEventListener = Collections.synchronizedMap(mutableMapOf<ListenerHandle, Set<String>>())

    @PostConstruct
    fun initListeners() {
        webappApi.doBeforeAppReady {
            // wait types initialization in main thread
            ecosTypesRegistry.initializationPromise().get(Duration.ofMinutes(100))
            val eventsRegistrationThread = thread(name = "cit-events-reg", start = true) {
                AuthContext.runAsSystem {
                    log.info { "===========Begin BPMN Citeck Event Listeners Registration===========" }

                    val deployedData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
                    val combinedSubscriptions = deployedData.subscriptions.combine()

                    for (subscription in combinedSubscriptions) {
                        if (eventListeners.containsKey(subscription.eventName)) {
                            throw IllegalStateException("Event ${subscription.eventName} already registered")
                        }

                        addSubscriptionListener(subscription)
                    }

                    registerConditionalEventsListeners(deployedData.conditionalEventsEcosTypes)

                    log.info {
                        buildString {
                            appendLine("\n===========Register BPMN Ecos Event Subscriptions===========")
                            for ((eventName, listener) in eventListeners) {
                                appendLine("Event: $eventName, atts: ${listener.first.attributes}")
                            }
                            appendLine("Conditional events: ${deployedData.conditionalEventsEcosTypes}")
                            appendLine("==============================================================")
                        }
                    }
                }
            }
            eventsRegistrationThread.join(Duration.ofSeconds(10))
        }
    }

    fun clean() {
        eventListeners.clear()
        conditionalEventListener.clear()
    }

    fun addSubscriptionsForDefRev(procDefRevId: UUID) {
        val deployedSubscriptionsData =
            camundaEventSubscriptionFinder.getDeployedSubscriptionsDataByProcDefRevId(procDefRevId)
        val combinedSubscriptions = deployedSubscriptionsData.subscriptions.combine()

        log.debug { "Add subscriptions for procDefRevId: $procDefRevId, subscriptions: \n$combinedSubscriptions" }

        for (subscription in combinedSubscriptions) {
            val (eventName, attributes) = subscription

            if (eventListeners.containsKey(eventName)) {
                if (requireUpdateExistingListener(subscription)) {
                    log.debug {
                        "BPMN Ecos Event Subscription Event: $eventName exists and need to update model"
                    }

                    // TODO: At current implementation we just re-registered listener.
                    // In future we can just update model - https://citeck.atlassian.net/browse/ECOSENT-2452.

                    val currentAtts = eventListeners[eventName]!!.first.attributes
                    val concatenatedAttsSubscription = subscription.copy(attributes = currentAtts + attributes)

                    eventListeners[eventName]?.second?.forEach { it.remove() }
                    eventListeners.remove(eventName)

                    addSubscriptionListener(concatenatedAttsSubscription)
                } else {
                    log.debug {
                        "BPMN Ecos Event Subscription Event: $eventName exists and no need to update"
                    }
                }
            } else {
                log.debug { "Register new BPMN Ecos Event Subscription Event: $eventName" }
                addSubscriptionListener(subscription)
            }
        }

        registerConditionalEventsListeners(deployedSubscriptionsData.conditionalEventsEcosTypes)
    }

    private fun addSubscriptionListener(subscription: CombinedEventSubscription) {
        val (eventName, attributes) = subscription

        val listenerEventNames = let {
            val eventType = EcosEventType.from(eventName)
            if (eventType == EcosEventType.UNDEFINED || eventType == EcosEventType.USER_EVENT) {
                listOf(eventName)
            } else {
                eventType.availableEventNames()
            }
        }

        log.debug {
            "BPMN Ecos Events Subscription <$eventName> add listeners to EventsService with " +
                "eventTypes: $listenerEventNames, atts: $attributes"
        }

        val excludeBpmnElementsOptimizationPredicate = Predicates.notEq(
            "record._type?id",
            "${AppName.EMODEL}/type@$BPMN_PROCESS_ELEMENT_TYPE"
        )

        val addedListeners = listenerEventNames.map { listenerEventName ->
            eventsService.addListener<ObjectData> {
                withDataClass(ObjectData::class.java)
                withEventType(listenerEventName)
                withAttributes(attributes.associateBy { it })
                withFilter(excludeBpmnElementsOptimizationPredicate)
                withTransactional(true)
                withAction { event ->
                    log.debug { "Receive subscription event: \n${Json.mapper.toPrettyString(event)}" }
                    camundaEventProcessor.processEvent(event.toGeneralEvent())
                }
            }
        }

        eventListeners[eventName] = subscription to addedListeners
    }

    private fun registerConditionalEventsListeners(ecosTypes: Set<EntityRef>) {
        log.debug { "Request to register conditional events for types: $ecosTypes" }

        if (conditionalEventListener.size > 1) {
            throw IllegalStateException("Conditional event listener can be only one")
        }

        val newTypes = ecosTypes.map { it.toString() }.toSet()
        val existingTypes = conditionalEventListener.values.flatten().toSet()

        val existingListener = if (conditionalEventListener.isNotEmpty()) {
            conditionalEventListener.keys.first()
        } else {
            null
        }

        if (existingListener != null && existingTypes.containsAll(newTypes)) {
            log.debug { "Conditional events already registered with types: $existingTypes" }
            return
        }

        val concatenatedTypes = existingTypes + newTypes

        val predicateFilterByTypes = Predicates.and(
            Predicates.`in`("record._type?id", concatenatedTypes),
        )

        // TODO: At current implementation we just re-registered listener.
        // In future we can just update model - https://citeck.atlassian.net/browse/ECOSENT-2452.
        existingListener?.remove()
        val listener = eventsService.addListener<RecordUpdatedEvent> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdatedEvent::class.java)
            withFilter(predicateFilterByTypes)
            withTransactional(true)
            withAction { updated ->
                log.debug { "Receive subscription conditional event: \n${Json.mapper.toPrettyString(updated)}" }
                bpmnConditionalEventsProcessor.processEvent(updated)
            }
        }

        log.debug { "Register new conditional events for types: $concatenatedTypes" }

        conditionalEventListener.clear()
        conditionalEventListener[listener] = concatenatedTypes
    }

    private fun requireUpdateExistingListener(newSubscription: CombinedEventSubscription): Boolean {
        val existingSubscription = eventListeners[newSubscription.eventName]!!.first
        return !existingSubscription.attributes.containsAll(newSubscription.attributes)
    }
}

private fun ObjectData.toGeneralEvent(): GeneralEvent {

    lateinit var id: String
    lateinit var time: Instant
    lateinit var type: String
    lateinit var user: String
    var workspace = ""
    val attributes = mutableMapOf<String, DataValue>()

    this.forEach { att, dataValue ->
        when (att) {
            EventSubscriptionCombiner.EVENT_ID_ATT -> id = dataValue.asText()
            EventSubscriptionCombiner.EVENT_TIME_ATT -> time = dataValue.getAsInstant()!!
            EventSubscriptionCombiner.EVENT_TYPE_ATT -> type = dataValue.asText()
            EventSubscriptionCombiner.EVENT_USER_ATT -> user = dataValue.asText()
            EventSubscriptionCombiner.EVENT_RECORD_WORKSPACE -> workspace = dataValue.asText()
            EventSubscriptionCombiner.EVENT_RECORD_ID_ATT -> attributes[EcosEventType.RECORD_ATT] = dataValue
            EventSubscriptionCombiner.EVENT_RECORD_TYPE_ID_ATT -> attributes[EcosEventType.RECORD_TYPE_ATT] = dataValue
            else -> attributes[att] = dataValue
        }
    }

    require(id.isNotBlank()) { "Event id is blank" }
    require(type.isNotBlank()) { "Event type is blank" }
    require(user.isNotBlank()) { "Event user is blank" }

    return GeneralEvent(id, time, type, user, workspace.ifBlank { ModelUtils.DEFAULT_WORKSPACE_ID }, attributes)
}

data class GeneralEvent(
    val id: String,
    val time: Instant,
    val type: String,
    val user: String,
    val workspace: String,
    val attributes: Map<String, DataValue>
)
