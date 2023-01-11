package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getEventSubscriptionsByEventNamesLikeStart
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.convert.toDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import java.io.Serializable
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Component
class CamundaEventSubscriptionFinder(
    private val bpmnProcService: BpmnProcService,
    private val camundaRuntimeService: RuntimeService,
    private val procDefService: ProcDefService,
    private val procDefRevRepo: ProcDefRevRepository,
    private val cachedEventSubscriptionProvider: CachedEventSubscriptionProvider
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun findAllDeployedSubscriptions(): List<EventSubscription> {
        val result: List<EventSubscription>

        val time = measureTimeMillis {
            val eventDefsOfDeploymentId = mutableMapOf<String, List<EventSubscription>>()

            val fillEventSubscriptions = fun(defRevs: List<ProcDefRevEntity>) {
                defRevs.map { it.toDto() }.forEach { defRev ->
                    val subscriptions = defRev.getBpmnSignalEventSubscriptions()
                    eventDefsOfDeploymentId[defRev.deploymentId] = subscriptions

                    cachedEventSubscriptionProvider.warmupEventSubscriptionCache(defRev.deploymentId, subscriptions)
                }
            }

            var slice = procDefRevRepo.queryAllByDeploymentIdIsNotNull(PageRequest.of(0, 20))

            fillEventSubscriptions(slice.content)

            while (slice.hasNext()) {
                slice = procDefRevRepo.queryAllByDeploymentIdIsNotNull(slice.nextPageable())
                fillEventSubscriptions(slice.content)
            }

            result = eventDefsOfDeploymentId.values.flatten()
        }

        log.debug { "Find All Deployed Subscriptions ${result.size} in $time ms" }

        return result
    }

    fun getActualCamundaSubscriptions(eventData: IncomingEventData): List<CamundaEventSubscription> {
        val result: List<CamundaEventSubscription>
        val time = measureTimeMillis {
            val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)
                .map { it.toComposedString() }

            if (log.isDebugEnabled) {
                log.debug { "Generate composed event names: $composedEventNames" }

                val allNames = camundaRuntimeService.createEventSubscriptionQuery().unlimitedList().map { it.eventName }
                log.debug { "All actual camunda event names: $allNames" }
            }

            result = camundaRuntimeService.getEventSubscriptionsByEventNamesLikeStart(composedEventNames)
                .map { sub ->

                    log.debug { "Process subscription: \n$sub" }

                    val definition = if (sub.processInstanceId.isNullOrBlank()) {
                        val defId = sub.configuration
                        if (defId.isNullOrBlank()) {
                            error("Cannot determine process definition id for event subscription: ${sub.id}")
                        }
                        bpmnProcService.getProcessDefinition(defId)
                    } else {
                        bpmnProcService.getProcessDefinitionByProcessInstanceId(sub.processInstanceId)
                    }

                    val deploymentId = definition?.deploymentId
                    if (deploymentId.isNullOrBlank()) {
                        error("Cannot determine deployment id for event subscription: ${sub.id}")
                    }

                    val eventSubscription = getExactEventSubscription(deploymentId, sub.activityId)

                    eventSubscription?.let {
                        CamundaEventSubscription(
                            id = sub.id,
                            event = it
                        )
                    }
                }.filterNotNull()
        }

        log.debug { "Get actual camunda subscriptions ${result.size} in $time ms. Result: \n$result" }

        return result
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

    fun getSubscriptionsByProcDefRevId(procDefRevId: UUID): List<EventSubscription> {
        val procDefRev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, procDefRevId)
            ?: error("Process definition revision not found by id: $procDefRevId")

        return procDefRev.getBpmnSignalEventSubscriptions()
    }
}

@Component
class CachedEventSubscriptionProvider(
    private val procDefService: ProcDefService,
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

        return defRev.getBpmnSignalEventSubscriptions()
    }

    @CachePut(cacheNames = [BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME], key = "#deploymentId")
    fun warmupEventSubscriptionCache(deploymentId: String, events: List<EventSubscription>): List<EventSubscription> {
        return events
    }
}

private val ktLog = KotlinLogging.logger {}

const val BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME =
    "bpmn-event-subscriptions-by-deployment-id-cache"

fun ProcDefRevDto.getBpmnSignalEventSubscriptions(): List<EventSubscription> {
    val defXml = this.data?.let { String(it, Charsets.UTF_8) }
        ?: error("Deployed definition cannot be empty")

    return try {
        BpmnIO.importEcosBpmn(defXml).signalsEventDefsMeta.map {
            EventSubscription(
                elementId = it.elementId,
                name = ComposedEventName.fromString(it.signalName),
                model = it.eventModel,
                predicate = it.eventFilterByPredicate?.let { pr -> Json.mapper.toString(pr) }
            )
        }
    } catch (e: Exception) {
        ktLog.debug(e) { "Cannot parse definition: ${this.id}" }

        emptyList()
    }
}

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
        if (Json.mapper.read(predicate) != Json.mapper.read(other.predicate)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + (predicate?.hashCode() ?: 0)
        return result
    }
}

data class CamundaEventSubscription(
    val id: String,
    val event: EventSubscription
)
