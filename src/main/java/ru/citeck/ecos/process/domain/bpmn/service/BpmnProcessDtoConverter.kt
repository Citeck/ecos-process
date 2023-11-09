package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.cockpit.impl.plugin.base.dto.CalledProcessInstanceDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.IncidentStatisticsDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.RecordsService
import javax.annotation.PostConstruct

@Component
internal class BpmnProcDtoConverterServiceProvider(
    val recordsService: RecordsService
) {
    @PostConstruct
    private fun init() {
        srv = this
    }
}

private lateinit var srv: BpmnProcDtoConverterServiceProvider

fun ProcessInstanceQuery.toCamundaQuery(): ProcessInstanceQueryDto {
    val query = ProcessInstanceQueryDto()

    if (businessKey.isNotBlank()) {
        query.businessKey = businessKey
    }

    if (bpmnDefEngine.isNotEmpty()) {
        val camundaProcessDefId = bpmnDefEngine.getLocalId()
        query.processDefinitionId = camundaProcessDefId
    }

    query.firstResult = page.skipCount
    query.maxResults = page.maxItems

    query.setSortBy("startTime")

    val order = if (sortBy.ascending) {
        "asc"
    } else {
        "desc"
    }
    query.setSortOrder(order)

    return query
}

fun ProcessInstanceDto.toProcessInstanceMeta(): ProcessInstanceMeta {
    return ProcessInstanceMeta(
        id = id,
        businessKey = businessKey ?: "",
        startTime = startTime.toInstant(),
        suspensionState = if (isSuspended) SuspensionState.SUSPENDED else SuspensionState.ACTIVE,
        incidentStatistics = incidents.map { it.toEcosIncidentStatistics() }
    )
}

fun CalledProcessInstanceDto.toCalledProcessInstanceMeta(): CalledProcessInstanceMeta {
    return CalledProcessInstanceMeta(
        id = id,
        businessKey = businessKey ?: "",
        startTime = startTime?.toInstant(),
        incidentStatistics = incidents.map { it.toEcosIncidentStatistics() },
        processDefinitionId = processDefinitionId ?: "",
        processDefinitionKey = processDefinitionKey ?: "",
        processDefinitionName = processDefinitionName ?: "",
        callActivityInstanceId = callActivityInstanceId ?: "",
        callActivityId = callActivityId ?: ""
    )
}

fun IncidentStatisticsDto.toEcosIncidentStatistics(): IncidentStatistics {
    return IncidentStatistics(
        type = incidentType,
        count = incidentCount.toLong()
    )
}
