package ru.citeck.ecos.process.domain.bpmn.process

import org.camunda.bpm.engine.history.HistoricProcessInstance
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.Incident
import org.camunda.bpm.engine.runtime.ProcessInstance
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_STATUS
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKSPACE
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

const val BPMN_ASYNC_START_PROCESS_QUEUE_NAME = "bpmn-process-async-start-process"

/**
 * WARNING!
 * This parameter should not be changed without deleting
 * of existing queue BPMN_ASYNC_START_PROCESS_QUEUE_NAME
 * because changing of retryDelayMs required queue recreation.
 *
 * @see ru.citeck.ecos.rabbitmq.RabbitMqChannel.declareQueuesWithRetrying
 */
// retry ~5 min
const val BPMN_ASYNC_START_PROCESS_QUEUE_RETRY_DELAY_MS = 500L

interface BpmnProcessService {

    fun startProcessAsync(request: StartProcessRequest)

    fun startProcess(request: StartProcessRequest): ProcessInstance

    fun deleteProcessInstance(
        processInstanceId: String,
        reason: String? = null,
        skipCustomListener: Boolean = false,
        skipIoMappings: Boolean = false
    )

    fun suspendProcess(processInstanceId: String)

    fun activateProcess(processInstanceId: String)

    fun setVariables(processInstanceId: String, variables: Map<String, Any?>)

    fun getIncidentsByProcessInstanceId(processInstanceId: String): List<Incident>

    fun getProcessInstanceActivityStatistics(processInstanceId: String): List<ActivityStatistics>

    /**
     * Returns the activity-instance tree for the given process instance, rooted at
     * the process definition. Returns `null` if the process instance is not active.
     *
     * Backed by Camunda's [org.camunda.bpm.engine.RuntimeService.getActivityInstance].
     */
    fun getProcessInstanceActivityTree(processInstanceId: String): ActivityInstanceNode?

    fun getProcessInstance(processInstanceId: String): ProcessInstance?

    fun getProcessInstancesForBusinessKey(businessKey: String): List<ProcessInstance>

    fun getProcessInstanceHistoricInstance(processInstanceId: String): HistoricProcessInstance?

    fun queryProcessInstancesMeta(query: ProcessInstanceQuery): List<ProcessInstanceMeta>

    fun getCalledProcessInstancesMeta(processInstanceId: String): List<CalledProcessInstanceMeta>

    fun queryProcessInstancesCount(query: ProcessInstanceQuery): Long

    fun getProcessDefinitionByProcessInstanceId(processInstanceId: String): ProcessDefinition?

    fun getProcessDefinition(processDefinitionId: String): ProcessDefinition?

    fun getProcessDefinitionsByKey(processKey: String): List<ProcessDefinition>
}

const val START_INSTRUCTION_START_BEFORE_ACTIVITY = "startBeforeActivity"
const val START_INSTRUCTION_START_AFTER_ACTIVITY = "startAfterActivity"

private val NON_SENSITIVE_VARIABLE_KEYS = setOf(
    BPMN_WORKFLOW_INITIATOR,
    BPMN_DOCUMENT_REF,
    BPMN_DOCUMENT_TYPE,
    BPMN_DOCUMENT_STATUS,
    BPMN_WORKSPACE
)

private fun maskSensitiveVariables(variables: Map<String, Any?>): Map<String, Any?> {
    return variables.entries.associate { (k, v) ->
        k to if (k in NON_SENSITIVE_VARIABLE_KEYS) v else "?"
    }
}

/**
 * This class is used to serialize/deserialize data in MQ.
 * All changes should be backward compatible.
 */
data class StartProcessRequest(
    val workspace: String = "",
    val processId: String,
    val businessKey: String? = null,
    val variables: Map<String, Any?> = emptyMap(),
    val startInstructions: List<StartInstruction> = emptyList()
) {
    override fun toString(): String {
        return "StartProcessRequest(workspace='$workspace', processId='$processId', " +
            "businessKey=$businessKey, variables=${maskSensitiveVariables(variables)}, " +
            "startInstructions=$startInstructions)"
    }
}

/**
 * Instructs process start to begin at the specified activity
 * instead of the default start event.
 *
 * @see org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder
 */
data class StartInstruction(
    val type: String,
    val activityId: String,
    val variables: Map<String, Any?> = emptyMap()
) {
    override fun toString(): String {
        return "StartInstruction(type='$type', activityId='$activityId', " +
            "variables=${maskSensitiveVariables(variables)})"
    }
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

data class CalledProcessInstanceMeta(
    val id: String,
    val businessKey: String = "",
    val startTime: Instant? = null,
    val incidentStatistics: List<IncidentStatistics>,
    val processDefinitionId: String = "",
    val processDefinitionKey: String = "",
    val processDefinitionName: String = "",
    val callActivityInstanceId: String = "",
    val callActivityId: String = ""
)

enum class SuspensionState {
    ACTIVE,
    SUSPENDED
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

/**
 * Activity-instance tree node. Field names mirror Camunda's
 * [org.camunda.bpm.engine.runtime.ActivityInstance] so the JSON shape is
 * directly compatible with Camunda REST `GET /process-instance/{id}/activity-instances`.
 */
data class ActivityInstanceNode(
    val id: String,
    val parentActivityInstanceId: String,
    val activityId: String,
    val activityType: String,
    val activityName: String,
    val processInstanceId: String,
    val processDefinitionId: String,
    val childActivityInstances: List<ActivityInstanceNode> = emptyList(),
    val childTransitionInstances: List<TransitionInstanceNode> = emptyList(),
    val executionIds: List<String> = emptyList(),
    val incidentIds: List<String> = emptyList()
)

data class TransitionInstanceNode(
    val id: String,
    val parentActivityInstanceId: String,
    val targetActivityId: String,
    val activityId: String,
    val activityName: String,
    val activityType: String,
    val processInstanceId: String,
    val processDefinitionId: String,
    val executionId: String,
    val incidentIds: List<String> = emptyList()
)
