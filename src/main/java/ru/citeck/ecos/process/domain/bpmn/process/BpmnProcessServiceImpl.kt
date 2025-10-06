package ru.citeck.ecos.process.domain.bpmn.process

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.camunda.bpm.cockpit.impl.plugin.base.dto.query.CalledProcessInstanceQueryDto
import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.exception.NullValueException
import org.camunda.bpm.engine.history.HistoricProcessInstance
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ActivityInstance
import org.camunda.bpm.engine.runtime.Incident
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.ProcessStartEvent
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.rabbitmq.RabbitMqChannel
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

@Service
class BpmnProcessServiceImpl(
    private val camundaRuntimeService: RuntimeService,
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService,
    private val bpmnEventEmitter: BpmnEventEmitter,
    private val processInstanceRestService: ProcessInstanceRestService,
    private val historyService: HistoryService,
    private val recordsService: RecordsService,
    private val meterRegistry: MeterRegistry,
    private val workspaceService: WorkspaceService,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection
) : BpmnProcessService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private lateinit var outcomeChannel: RabbitMqChannel

    init {
        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            outcomeChannel = channel
            channel.declareQueuesWithRetrying(
                BPMN_ASYNC_START_PROCESS_QUEUE_NAME,
                BPMN_ASYNC_START_PROCESS_QUEUE_RETRY_DELAY_MS
            )
        }
    }

    override fun startProcessAsync(request: StartProcessRequest) {
        val requestWithInitiator = request.copy(
            variables = request.variables.toMutableMap().apply {
                this[BPMN_WORKFLOW_INITIATOR] = AuthContext.getCurrentUser()
            }
        )

        outcomeChannel.publishMsg(BPMN_ASYNC_START_PROCESS_QUEUE_NAME, requestWithInitiator)
    }

    override fun startProcess(request: StartProcessRequest): ProcessInstance {

        var processKey = request.processId
        if (!workspaceService.isWorkspaceWithGlobalArtifacts(request.workspace)) {
            val wsSysId = workspaceService.getWorkspaceSystemId(request.workspace)
            processKey = wsSysId + ProcUtils.PROC_KEY_WS_DELIM + processKey
        }

        with(request) {

            val timer = Timer.start()
            val tag = Tag.of("processKey", processKey)

            val processInstance: ProcessInstance
            val time = measureTimeMillis {
                val definitionId: String
                val getDefinitionIdTime = measureTimeMillis {
                    definitionId = recordsService.getAtt(
                        EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, processKey),
                        "definition.id"
                    ).asText()
                }

                val defIdInWs = workspaceService.convertToIdInWs(definitionId)

                val definition: ProcDefWithDataDto
                val getDefTime = measureTimeMillis {
                    definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, defIdInWs))
                        ?: throw IllegalArgumentException(
                            "Process definition with id '$definitionId' not found for " +
                                "process key $processKey"
                        )
                }

                check(definition.enabled) {
                    "Starting a disabled process is not possible"
                }

                val processVariables = variables.toMutableMap()

                val workflowInitiator = variables[BPMN_WORKFLOW_INITIATOR].takeIf { it.toString().isNotBlank() }
                    ?: AuthContext.getCurrentUser()
                processVariables[BPMN_WORKFLOW_INITIATOR] = workflowInitiator

                val instance: ProcessInstance
                val startProcessTime = measureTimeMillis {
                    instance =
                        camundaRuntimeService.startProcessInstanceByKey(processKey, businessKey, processVariables)
                }

                val emitProcessStartTime = measureTimeMillis {
                    bpmnEventEmitter.emitProcessStart(
                        ProcessStartEvent(
                            processKey = processKey,
                            processInstanceId = instance.id,
                            processDefinitionId = instance.processDefinitionId,
                            document = EntityRef.valueOf(businessKey)
                        )
                    )
                }

                processInstance = instance

                log.trace {
                    "Start process ${processInstance.id} times: \n" +
                        "getDefinitionIdTime=$getDefinitionIdTime, \n" +
                        "getDefTime=$getDefTime, \n" +
                        "startProcessTime=$startProcessTime, \n" +
                        "emitProcessStartTime=$emitProcessStartTime"
                }
            }

            log.debug { "Start process ${processInstance.id} in $time ms" }

            timer.stop(
                Timer.builder("bpmn.process.start.time")
                    .description("Time to start BPMN process")
                    .tags(listOf(tag))
                    .register(meterRegistry)
            )

            return processInstance
        }
    }

    override fun deleteProcessInstance(
        processInstanceId: String,
        reason: String?,
        skipCustomListener: Boolean,
        skipIoMappings: Boolean
    ) {
        camundaRuntimeService.deleteProcessInstance(
            processInstanceId,
            reason,
            skipCustomListener,
            true,
            skipIoMappings,
            false
        )
    }

    override fun suspendProcess(processInstanceId: String) {
        getProcessInstance(processInstanceId) ?: error("Process instance $processInstanceId not found")

        camundaRuntimeService.updateProcessInstanceSuspensionState()
            .byProcessInstanceId(processInstanceId)
            .suspend()
    }

    override fun activateProcess(processInstanceId: String) {
        getProcessInstance(processInstanceId) ?: error("Process instance $processInstanceId not found")

        camundaRuntimeService.updateProcessInstanceSuspensionState()
            .byProcessInstanceId(processInstanceId)
            .activate()
    }

    override fun setVariables(processInstanceId: String, variables: Map<String, Any?>) {
        camundaRuntimeService.setVariables(processInstanceId, variables)
    }

    override fun getIncidentsByProcessInstanceId(processInstanceId: String): List<Incident> {
        return camundaRuntimeService.createIncidentQuery()
            .processInstanceId(processInstanceId)
            .list()
    }

    override fun getProcessInstanceActivityStatistics(processInstanceId: String): List<ActivityStatistics> {
        val activitiesStats = mutableMapOf<String, ActivityStatistics>()

        val rootInstance = camundaRuntimeService.getActivityInstance(processInstanceId) ?: return emptyList()

        fun extractStatFromActivity(activityInstance: ActivityInstance) {
            if (activityInstance.activityType != "processDefinition" &&
                activityInstance.activityType != "multiInstanceBody"
            ) {
                val currentActivityStat = activitiesStats.computeIfAbsent(activityInstance.activityId) {
                    ActivityStatistics(
                        activityId = activityInstance.activityId,
                        instances = 0,
                        incidentStatistics = mutableListOf()
                    )
                }.apply {
                    instances += 1
                }

                val incidentsCountMap = activityInstance.incidents
                    ?.groupingBy { it.incidentType }
                    ?.eachCount() ?: emptyMap()

                for ((type, count) in incidentsCountMap) {
                    val incidentStat = currentActivityStat.incidentStatistics.find { it.type == type }
                    if (incidentStat != null) {
                        incidentStat.count += count
                    } else {
                        currentActivityStat.incidentStatistics +=
                            IncidentStatistics(
                                type = type,
                                count = count.toLong()
                            )
                    }
                }

                activitiesStats[activityInstance.activityId] = currentActivityStat
            }

            activityInstance.childActivityInstances.forEach {
                extractStatFromActivity(it)
            }
        }

        rootInstance.childActivityInstances.forEach {
            extractStatFromActivity(it)
        }

        return activitiesStats.values.toList()
    }

    override fun getProcessInstance(processInstanceId: String): ProcessInstance? {
        return camundaRuntimeService.createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult()
    }

    override fun getProcessInstancesForBusinessKey(businessKey: String): List<ProcessInstance> {
        return camundaRuntimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(businessKey)
            .list()
    }

    override fun getProcessInstanceHistoricInstance(processInstanceId: String): HistoricProcessInstance? {
        return historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult()
    }

    override fun queryProcessInstancesMeta(query: ProcessInstanceQuery): List<ProcessInstanceMeta> {
        val camundaQuery = query.toCamundaQuery()
        val result = processInstanceRestService.queryProcessInstances(
            query.toCamundaQuery(),
            camundaQuery.firstResult,
            camundaQuery.maxResults
        )
            .map { it.toProcessInstanceMeta() }
            .toList()

        log.debug { "Query process instances: \n$query, \ncount: ${result.size} \nresult: $result" }

        return result
    }

    override fun getCalledProcessInstancesMeta(processInstanceId: String): List<CalledProcessInstanceMeta> {
        return processInstanceRestService.getProcessInstance(processInstanceId)
            .queryCalledProcessInstances(CalledProcessInstanceQueryDto())
            .map { it.toCalledProcessInstanceMeta() }
    }

    override fun queryProcessInstancesCount(query: ProcessInstanceQuery): Long {
        return processInstanceRestService.queryProcessInstancesCount(query.toCamundaQuery()).count
    }

    override fun getProcessDefinitionByProcessInstanceId(processInstanceId: String): ProcessDefinition? {
        val processInstance = getProcessInstance(processInstanceId) ?: return null
        return getProcessDefinition(processInstance.processDefinitionId)
    }

    override fun getProcessDefinition(processDefinitionId: String): ProcessDefinition? {
        return try {
            camundaRepositoryService.getProcessDefinition(processDefinitionId)
        } catch (e: NullValueException) {
            log.debug(e) { "Error while getting process definition by id: $processDefinitionId" }
            null
        }
    }

    override fun getProcessDefinitionsByKey(processKey: String): List<ProcessDefinition> {
        return camundaRepositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).list().toList()
    }
}
