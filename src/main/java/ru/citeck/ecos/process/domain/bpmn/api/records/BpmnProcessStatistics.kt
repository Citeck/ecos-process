package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.value.AttValue
import javax.annotation.PostConstruct

//TODO: remove
@Component
class BpmnProcessStatisticsProvider(
    val camundaRuntimeService: RuntimeService
) {

    @PostConstruct
    private fun init() {
        srv = this
    }

}

private lateinit var srv: BpmnProcessStatisticsProvider

class BpmnProcessStatisticsValue(
    private val processKey: String
) : AttValue {

    private val statistics: BpmnProcessStatistics by lazy {
        if (processKey.isBlank()) {
            BpmnProcessStatistics(0, 0)
        }
        val incidentsCount = srv.camundaRuntimeService.createIncidentQuery()
            .processDefinitionKeyIn(processKey)
            .count()
        val instancesCount = srv.camundaRuntimeService.createProcessInstanceQuery()
            .processDefinitionKey(processKey)
            .count()
        BpmnProcessStatistics(incidentsCount, instancesCount)
    }

    override fun getAtt(name: String): Any? {
        return when (name) {
            "incidentsCount" -> statistics.incidentsCount
            "instancesCount" -> statistics.instancesCount
            else -> null
        }
    }
}

class BpmnProcessDefinitionStatisticsValue(
    private val camundaDefinitionId: String
) : AttValue {

    private val statistics: BpmnProcessStatistics by lazy {
        if (camundaDefinitionId.isBlank()) {
            BpmnProcessStatistics(0, 0)
        }

        val incidentsCount = srv.camundaRuntimeService.createIncidentQuery()
            .processDefinitionId(camundaDefinitionId)
            .count()
        val instancesCount = srv.camundaRuntimeService.createProcessInstanceQuery()
            .processDefinitionId(camundaDefinitionId)
            .count()
        BpmnProcessStatistics(incidentsCount, instancesCount)
    }

    override fun getAtt(name: String): Any? {
        return when (name) {
            "incidentsCount" -> statistics.incidentsCount
            "instancesCount" -> statistics.instancesCount
            else -> null
        }
    }

}

