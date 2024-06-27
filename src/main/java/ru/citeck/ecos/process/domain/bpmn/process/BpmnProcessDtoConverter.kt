package ru.citeck.ecos.process.domain.bpmn.process

import org.camunda.bpm.cockpit.impl.plugin.base.dto.CalledProcessInstanceDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.IncidentStatisticsDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.ProcessInstanceDto
import org.camunda.bpm.cockpit.impl.plugin.base.dto.query.ProcessInstanceQueryDto

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
