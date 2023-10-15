package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.engine.history.HistoricProcessInstance
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.Incident
import org.camunda.bpm.engine.runtime.ProcessInstance
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

interface BpmnProcessService {

    fun startProcess(processKey: String, businessKey: String? = null, variables: Map<String, Any?>): ProcessInstance

    fun deleteProcessInstance(
        processInstanceId: String,
        skipCustomListener: Boolean = false,
        skipIoMappings: Boolean = false
    )

    fun suspendProcess(processInstanceId: String)

    fun activateProcess(processInstanceId: String)

    fun setVariables(processInstanceId: String, variables: Map<String, Any?>)

    fun getIncidentsByProcessInstanceId(processInstanceId: String): List<Incident>

    fun getProcessInstanceActivityStatistics(processInstanceId: String): List<ActivityStatistics>

    fun getProcessInstance(processInstanceId: String): ProcessInstance?

    fun getProcessInstancesForBusinessKey(businessKey: String): List<ProcessInstance>

    fun getProcessInstanceHistoricInstance(processInstanceId: String): HistoricProcessInstance?

    fun queryProcessInstancesMeta(query: ProcessInstanceQuery): List<ProcessInstanceMeta>

    fun queryProcessInstancesCount(query: ProcessInstanceQuery): Long

    fun getProcessDefinitionByProcessInstanceId(processInstanceId: String): ProcessDefinition?

    fun getProcessDefinition(processDefinitionId: String): ProcessDefinition?

    fun getProcessDefinitionsByKey(processKey: String): List<ProcessDefinition>
}

data class ProcessInstanceQuery(
    val businessKey: String = "",
    val bpmnDefEngine: EntityRef = EntityRef.EMPTY,

    val page: QueryPage,
    val sortBy: SortBy
)

data class ProcessInstanceMeta(
    val id: String,
    val businessKey: String = "",
    val startTime: Instant,
    val suspensionState: SuspensionState,
    val incidentStatistics: List<IncidentStatistics>
)

enum class SuspensionState {
    ACTIVE, SUSPENDED
}

data class IncidentStatistics(
    val type: String,
    var count: Long
)

data class BpmnProcessStatistics(
    val incidentsCount: Long,
    val instancesCount: Long
)

data class ActivityStatistics(
    val activityId: String,
    var instances: Long,
    var incidentStatistics: List<IncidentStatistics> = emptyList()
)
