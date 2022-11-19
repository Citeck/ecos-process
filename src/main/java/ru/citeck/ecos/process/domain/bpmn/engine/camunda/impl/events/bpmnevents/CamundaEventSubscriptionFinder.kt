package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.signalName
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.util.*

/**
 * @author Roman Makarskiy
 */
@Component
class CamundaEventSubscriptionFinder(
    private val camundaRepositoryService: RepositoryService,
    private val bpmnProcService: BpmnProcService,
    private val camundaRuntimeService: RuntimeService,
    private val procDefService: ProcDefService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun findAllDeployedSubscriptions(): List<EventSubscription> {
        val definitions = camundaRepositoryService.createProcessDefinitionQuery()
            .unlimitedList()

        val deploymentIds = mutableSetOf<String>()

        val subscriptions = procDefService.findAllRevisionsWhereDeploymentIdIsNotNull()
            .map { defRev ->
                deploymentIds.add(defRev.deploymentId!!)

                defRev.getBpmnSignalEventDefs()
            }.flatten()
            .map {
                EventSubscription(
                    name = ComposedEventName.fromString(it.signalName),
                    model = it.eventModel,
                    predicate = it.eventFilterByPredicate
                )
            }

        /*val allCamundaSubcriptions = camundaRuntimeService.createEventSubscriptionQuery()
            .unlimitedList().forEach { sub ->
                val procInstanceId = sub.processInstanceId
                val def = bpmnProcService.getProcessDefinitionByProcessInstanceId(procInstanceId)
                val deploymentId = def!!.deploymentId

                println(deploymentId)
            }*/

        return subscriptions
    }

    fun getSubscriptionsByProcDefRevId(procDefRevId: UUID): List<EventSubscription> {
        val procDefRev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, procDefRevId)
            ?: error("Process definition revision not found by id: $procDefRevId")

        return procDefRev.getBpmnSignalEventDefs()
            .map {
                EventSubscription(
                    name = ComposedEventName.fromString(it.signalName),
                    model = it.eventModel,
                    predicate = it.eventFilterByPredicate
                )
            }
    }

    private fun ProcDefRevDto.getBpmnSignalEventDefs(): List<BpmnSignalEventDef> {
        val defXml = this.data?.let { String(it, Charsets.UTF_8) }
            ?: error("Deployed definition cannot be empty")

        return try {
            BpmnIO.importEcosBpmn(defXml).signals.map {
                it.eventDef
            }
        } catch (e: Exception) {
            log.debug(e) { "Cannot parse definition: ${this.id}" }

            emptyList()
        }
    }
}

data class EventSubscription(
    val name: ComposedEventName,
    val model: Map<String, String>,
    val predicate: Predicate? = null
)
