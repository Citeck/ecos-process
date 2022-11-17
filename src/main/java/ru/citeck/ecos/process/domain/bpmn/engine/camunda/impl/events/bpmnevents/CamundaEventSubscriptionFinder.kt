package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.signalName
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.records2.predicate.model.Predicate

/**
 * @author Roman Makarskiy
 */
@Component
class CamundaEventSubscriptionFinder(
    private val camundaRepositoryService: RepositoryService,
    private val procDefRevRepo: ProcDefRevRepository,
    private val bpmnProcService: BpmnProcService,
    private val camundaRuntimeService: RuntimeService
) {

    fun findAllDeployedSubscription(): List<EventSubscription> {
        val definitions = camundaRepositoryService.createProcessDefinitionQuery()
            .unlimitedList()

        val deploymentIds = mutableSetOf<String>()

        val subscriptions = procDefRevRepo.queryAllByDeploymentIdIsNotNull()
            .map { defRev ->
                deploymentIds.add(defRev.deploymentId!!)

                val defXml = defRev.data?.let { String(it, Charsets.UTF_8) }
                    ?: error("Deployed definition cannot be empty")

                BpmnIO.importEcosBpmn(defXml).signals.map {
                    it.eventDef
                }
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

}

data class EventSubscription(
    val name: ComposedEventName,
    val model: Map<String, String>,
    val predicate: Predicate?
)

