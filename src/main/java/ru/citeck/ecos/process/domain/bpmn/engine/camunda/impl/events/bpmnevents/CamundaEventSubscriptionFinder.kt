package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaMyBatisExtension
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getConditionalEventSubscriptionsByProcessInstanceIds
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getEventSubscriptionsByEventNamesLikeStart
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.procdef.convert.toDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.io.Serializable
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Component
class CamundaEventSubscriptionFinder(
    private val bpmnProcessService: BpmnProcessService,
    private val camundaRuntimeService: RuntimeService,
    private val procDefService: ProcDefService,
    private val procDefRevRepo: ProcDefRevRepository,
    private val cachedEventSubscriptionProvider: CachedEventSubscriptionProvider,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val camundaMyBatisExtension: CamundaMyBatisExtension,
    private val bpmnIO: BpmnIO
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun findDeployedSubscriptionsData(): DeployedSubscriptionsData {
        val ecosTypes = mutableSetOf<EntityRef>()
        val subscriptions: List<EventSubscription>

        val time = measureTimeMillis {
            val eventDefsOfDeploymentId = mutableMapOf<String, List<EventSubscription>>()

            val fillEventSubscriptions = fun(defRevs: List<ProcDefRevEntity>) {
                defRevs.map { it.toDto() }.forEach { defRev ->
                    if (defRev.deploymentId.isNullOrBlank()) {
                        log.warn {
                            "Deployment id is null or blank for proc def rev: ${defRev.id}. " +
                                "Its wrong, because we find only deployed proc defs"
                        }
                        return@forEach
                    }

                    val deployedData = defRev.parseDeployedSubscriptionsData(bpmnIO, procDefRevDataProvider)
                    eventDefsOfDeploymentId[defRev.deploymentId!!] = deployedData.subscriptions
                    ecosTypes.addAll(deployedData.conditionalEventsEcosTypes)

                    cachedEventSubscriptionProvider.warmupEventSubscriptionCache(
                        defRev.deploymentId!!,
                        deployedData.subscriptions
                    )
                }
            }

            var slice = procDefRevRepo.queryAllByDeploymentIdIsNotNull(PageRequest.of(0, 20))

            fillEventSubscriptions(slice.content)

            while (slice.hasNext()) {
                slice = procDefRevRepo.queryAllByDeploymentIdIsNotNull(slice.nextPageable())
                fillEventSubscriptions(slice.content)
            }

            subscriptions = eventDefsOfDeploymentId.values.flatten()
        }

        log.debug { "Find All Deployed Subscriptions ${subscriptions.size} in $time ms" }

        return DeployedSubscriptionsData(ecosTypes, subscriptions)
    }

    fun getActualCamundaSubscriptions(eventData: IncomingEventData): List<CamundaEventSubscription> {
        val result: List<CamundaEventSubscription>
        val time = measureTimeMillis {
            val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)
                .map { it.toComposedString() }

            if (log.isTraceEnabled) {
                log.trace { "Generate composed event names: $composedEventNames" }

                val allNames = camundaRuntimeService.createEventSubscriptionQuery().unlimitedList().map { it.eventName }
                log.trace { "All actual camunda event names: $allNames" }
            }

            result =
                camundaRuntimeService.getEventSubscriptionsByEventNamesLikeStart(
                    composedEventNames,
                    camundaMyBatisExtension
                ).mapNotNull { sub ->

                    log.debug { "Process subscription: \n$sub" }

                    val deploymentId = getProcDefDeploymentIdForSubscription(sub)
                    if (deploymentId.isBlank()) {
                        error("Cannot determine deployment id for event subscription: ${sub.id}")
                    }

                    val eventSubscription = getExactEventSubscription(deploymentId, sub.activityId)

                    eventSubscription?.let {
                        CamundaEventSubscription(
                            id = sub.id,
                            event = it
                        )
                    }
                }
        }

        log.debug { "Get actual camunda subscriptions ${result.size} in $time ms. Result: \n$result" }

        return result
    }

    fun getActualCamundaConditionalEventsForBusinessKey(businessKey: String): List<CamundaConditionalEvent> {
        if (businessKey.isEmpty()) {
            return emptyList()
        }

        val result: List<CamundaConditionalEvent>
        val time = measureTimeMillis {
            val processes = bpmnProcessService.getProcessInstancesForBusinessKey(businessKey).map {
                it.processInstanceId
            }

            result = camundaRuntimeService.getConditionalEventSubscriptionsByProcessInstanceIds(
                processes,
                camundaMyBatisExtension
            )
                .mapNotNull { sub ->
                    val deploymentId = getProcDefDeploymentIdForSubscription(sub)
                    if (deploymentId.isBlank()) {
                        error("Cannot determine deployment id for event subscription: ${sub.id}")
                    }

                    val conditionalEvents = getExactConditionalEvent(deploymentId, sub.activityId)

                    conditionalEvents?.let {
                        CamundaConditionalEvent(
                            id = sub.id,
                            event = it
                        )
                    }
                }
        }

        log.debug { "Get actual camunda conditional events ${result.size} in $time ms. Result: \n$result" }

        return result
    }

    private fun getProcDefDeploymentIdForSubscription(subscription: EventSubscriptionEntity): String {
        return if (Context.getCommandContext() != null) {
            subscription.processDefinition?.deploymentId ?: ""
        } else {
            val definition = if (subscription.processInstanceId.isNullOrBlank()) {
                val defId = subscription.configuration
                if (defId.isNullOrBlank()) {
                    error("Cannot determine process definition id for event subscription: ${subscription.id}")
                }
                bpmnProcessService.getProcessDefinition(defId)
            } else {
                bpmnProcessService.getProcessDefinitionByProcessInstanceId(subscription.processInstanceId)
            }
            definition?.deploymentId ?: ""
        }
    }

    private fun getExactEventSubscription(deploymentId: String, elementId: String): EventSubscription? {
        val events = cachedEventSubscriptionProvider.getEventSubscriptionsByDeploymentId(deploymentId)

        log.debug { "Events subscriptions by deployment id $deploymentId: \n$events" }

        val found = events.filter {
            elementId == it.elementId
        }

        if (found.size > 1) {
            error("Found more than one event subscription for deploymentId $deploymentId elementId $elementId")
        }

        return found.firstOrNull()
    }

    private fun getExactConditionalEvent(deploymentId: String, elementId: String): ConditionalEvent? {
        val events = cachedEventSubscriptionProvider.getConditionalEventSubscriptionsByDeploymentId(deploymentId)

        log.debug { "Conditional events by deployment id $deploymentId: \n$events" }

        val found = events.filter {
            elementId == it.elementId
        }

        if (found.size > 1) {
            error("Found more than one conditional event for deploymentId $deploymentId elementId $elementId")
        }

        return found.firstOrNull()
    }

    fun getDeployedSubscriptionsDataByProcDefRevId(procDefRevId: UUID): DeployedSubscriptionsData {
        val procDefRev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, procDefRevId)
            ?: error("Process definition revision not found by id: $procDefRevId")

        return procDefRev.parseDeployedSubscriptionsData(bpmnIO, procDefRevDataProvider)
    }
}

@Component
class CachedEventSubscriptionProvider(
    private val procDefService: ProcDefService,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val bpmnIO: BpmnIO
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Cacheable(cacheNames = [BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME])
    fun getEventSubscriptionsByDeploymentId(deploymentId: String): List<EventSubscription> {
        val defRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)
        if (defRev == null) {
            log.error { "Process definition revision is null. DeploymentId: $deploymentId" }
            return emptyList()
        }

        return defRev.parseDeployedSubscriptionsData(bpmnIO, procDefRevDataProvider).subscriptions
    }

    @Cacheable(cacheNames = [BPMN_CONDITIONAL_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME])
    fun getConditionalEventSubscriptionsByDeploymentId(deploymentId: String): List<ConditionalEvent> {
        val defRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)
        if (defRev == null) {
            log.error { "Process definition revision is null. DeploymentId: $deploymentId" }
            return emptyList()
        }

        return defRev.getBpmnConditionalEvents(bpmnIO, procDefRevDataProvider)
    }

    @CachePut(cacheNames = [BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME], key = "#deploymentId")
    fun warmupEventSubscriptionCache(deploymentId: String, events: List<EventSubscription>): List<EventSubscription> {
        return events
    }
}

private val ktLog = KotlinLogging.logger {}

const val BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME =
    "bpmn-event-subscriptions-by-deployment-id-cache"

const val BPMN_CONDITIONAL_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME =
    "bpmn-conditional-event-subscriptions-by-deployment-id-cache"

private fun ProcDefRevDto.parseDeployedSubscriptionsData(
    bpmnIO: BpmnIO,
    dataProvider: ProcDefRevDataProvider
): DeployedSubscriptionsData {
    val defXml = String(this.loadData(dataProvider), Charsets.UTF_8)
    val definition = try {
        bpmnIO.importEcosBpmn(defXml)
    } catch (e: Exception) {
        ktLog.debug(e) { "Cannot parse definition: ${this.id}" }
        return DeployedSubscriptionsData(emptySet(), emptyList())
    }

    val definitionHasConditionalEventWithReactOnDocumentChange = definition.conditionalEventDefsMeta.any {
        it.reactOnDocumentChange
    }

    val ecosTypes = mutableSetOf<EntityRef>()
    if (definitionHasConditionalEventWithReactOnDocumentChange) {
        ecosTypes.add(definition.ecosType)
        definition.collaboration?.participants?.forEach { participant ->
            ecosTypes.add(participant.ecosType)
        }
    }
    val notEmptyTypes = ecosTypes.filter { it.isNotEmpty() }.toSet()

    val subscriptions = definition.signalsEventDefsMeta.map {
        EventSubscription(
            elementId = it.elementId,
            name = ComposedEventName.fromString(it.signalName),
            model = it.eventModel,
            predicate = it.eventFilterByPredicate?.let { pr -> Json.mapper.toString(pr) }
        )
    }

    return DeployedSubscriptionsData(notEmptyTypes, subscriptions)
}

private fun ProcDefRevDto.getBpmnConditionalEvents(bpmnIO: BpmnIO, dataProvider: ProcDefRevDataProvider): List<ConditionalEvent> {
    val defXml = String(this.loadData(dataProvider), Charsets.UTF_8)

    return try {
        bpmnIO.importEcosBpmn(defXml).conditionalEventDefsMeta.map { conditionalEvent ->
            ConditionalEvent(
                reactOnDocumentChange = conditionalEvent.reactOnDocumentChange,
                documentVariables = conditionalEvent.documentVariables.filter { it.isNotBlank() },
                elementId = conditionalEvent.elementId
            )
        }
    } catch (e: Exception) {
        ktLog.debug(e) { "Cannot parse definition: ${this.id}" }

        emptyList()
    }
}

data class DeployedSubscriptionsData(
    val conditionalEventsEcosTypes: Set<EntityRef>,
    val subscriptions: List<EventSubscription>
)

data class EventSubscription(
    val elementId: String,
    val name: ComposedEventName,
    val model: Map<String, String>,

    // store predicates as string for Hazelcast serialization
    val predicate: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventSubscription) return false

        if (name != other.name) return false
        if (model != other.model) return false
        if (getPredicateValue() != other.getPredicateValue()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + getPredicateValue().hashCode()
        return result
    }

    private fun getPredicateValue(): Predicate {
        if (predicate == null) {
            return VoidPredicate.INSTANCE
        }
        return Json.mapper.read(predicate, Predicate::class.java) ?: VoidPredicate.INSTANCE
    }
}

data class CamundaEventSubscription(
    val id: String,
    val event: EventSubscription
)

data class ConditionalEvent(
    val reactOnDocumentChange: Boolean,
    val elementId: String,
    val documentVariables: List<String>
)

data class CamundaConditionalEvent(
    val id: String,
    val event: ConditionalEvent
)
