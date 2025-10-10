package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.interceptor.CommandContext
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.util.EnsureUtil
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_EVENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.api.entity.EntityRef

class SendSignalEventByIdCmd(
    private val signalId: String,
    eventData: BpmnDataValue,
    private val procDefService: ProcDefService,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val bpmnIO: BpmnIO
) : Command<Unit> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val notifyEventVariables = mapOf(BPMN_EVENT to eventData)

    private val startProcessVariables = let {
        val data = mutableMapOf<String, Any>(BPMN_EVENT to eventData)

        val recordAtt = eventData[EcosEventType.RECORD_ATT].asText()
        if (recordAtt.isNotBlank()) {
            data[BPMN_DOCUMENT_REF] = recordAtt
        }

        val recordTypeRef = EntityRef.valueOf(eventData[EcosEventType.RECORD_TYPE_ATT].asText())
        val localId = recordTypeRef.getLocalId()
        if (localId.isNotBlank()) {
            data[BPMN_DOCUMENT_TYPE] = localId
        }

        data.toMap()
    }

    private val businessKey = let {
        val recordAtt = eventData[EcosEventType.RECORD_ATT].asText()
        recordAtt.ifBlank {
            null
        }
    }

    override fun execute(commandContext: CommandContext) {
        sendSignal(commandContext)
    }

    private fun sendSignal(commandContext: CommandContext) {
        val signalEventSubscriptions = findSignalEventSubscriptions(commandContext)
        val catchSignalEventSubscription = filterIntermediateSubscriptions(signalEventSubscriptions)
        val startSignalEventSubscriptions = filterStartSubscriptions(signalEventSubscriptions)

        val processDefinitions: Map<String, ProcessDefinitionEntity> =
            getProcessDefinitionsOfSubscriptions(startSignalEventSubscriptions)
        checkAuthorizationOfCatchSignals(commandContext, catchSignalEventSubscription)
        checkAuthorizationOfStartSignals(commandContext, startSignalEventSubscriptions, processDefinitions)

        notifyExecutions(catchSignalEventSubscription)
        startProcessInstances(startSignalEventSubscriptions, processDefinitions)
    }

    private fun findSignalEventSubscriptions(commandContext: CommandContext): List<EventSubscriptionEntity> {
        val eventSubscriptionManager = commandContext.eventSubscriptionManager
        val subs = eventSubscriptionManager.findEventSubscriptionById(signalId)
        return listOfNotNull(subs)
    }

    private fun checkAuthorizationOfCatchSignals(
        commandContext: CommandContext,
        catchSignalEventSubscription: List<EventSubscriptionEntity>
    ) {
        // check authorization for each fetched signal event
        for (event in catchSignalEventSubscription) {
            val processInstanceId = event.processInstanceId
            for (checker in commandContext.processEngineConfiguration.commandCheckers) {
                checker.checkUpdateProcessInstanceById(processInstanceId)
            }
        }
    }

    private fun checkAuthorizationOfStartSignals(
        commandContext: CommandContext,
        startSignalEventSubscriptions: List<EventSubscriptionEntity>,
        processDefinitions: Map<String, ProcessDefinitionEntity>
    ) {
        // check authorization for process definition
        for (signalStartEventSubscription in startSignalEventSubscriptions) {
            val processDefinition = processDefinitions[signalStartEventSubscription.id]
            if (processDefinition != null) {
                for (checker in commandContext.processEngineConfiguration.commandCheckers) {
                    checker.checkCreateProcessInstance(processDefinition)
                }
            }
        }
    }

    private fun getProcessDefinitionsOfSubscriptions(
        startSignalEventSubscriptions: List<EventSubscriptionEntity>
    ): Map<String, ProcessDefinitionEntity> {
        val deploymentCache = Context.getProcessEngineConfiguration().deploymentCache
        val processDefinitions: MutableMap<String, ProcessDefinitionEntity> = HashMap()
        for (eventSubscription in startSignalEventSubscriptions) {
            val processDefinitionId = eventSubscription.configuration
            EnsureUtil.ensureNotNull(
                "Configuration of signal start event subscription '" + eventSubscription.id +
                    "' contains no process definition id.",
                processDefinitionId
            )
            val processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId)
            if (processDefinition != null && !processDefinition.isSuspended) {
                if (isProcessEnabled(processDefinition)) {
                    processDefinitions[eventSubscription.id] = processDefinition
                }
            }
        }
        return processDefinitions
    }

    /**
     * Check if process enabled flag (ecos:enabled) is set in Citeck Bpmn.
     *
     * - If process does not have participants, then use definition enabled flag.
     * - If definition enabled flag is false, then all inside processes are disabled.
     */
    private fun isProcessEnabled(processDefinition: ProcessDefinitionEntity): Boolean {
        return try {
            val deploymentId = processDefinition.deploymentId
            val procDefRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)
                ?: return false

            val defXml = String(procDefRev.loadData(procDefRevDataProvider), Charsets.UTF_8)
            val definition = bpmnIO.importEcosBpmn(defXml)
            if (definition.enabled.not()) {
                return false
            }

            definition.collaboration?.participants?.find {
                it.processRef == processDefinition.key
            }?.enabled ?: definition.enabled
        } catch (e: Exception) {
            log.warn(e) { "Cannot determine enabled state of process definition ${processDefinition.id}" }
            false
        }
    }

    private fun notifyExecutions(catchSignalEventSubscription: List<EventSubscriptionEntity>) {
        for (signalEventSubscriptionEntity in catchSignalEventSubscription) {
            if (isActiveEventSubscription(signalEventSubscriptionEntity)) {
                signalEventSubscriptionEntity.eventReceived(notifyEventVariables, false)
            }
        }
    }

    private fun isActiveEventSubscription(signalEventSubscriptionEntity: EventSubscriptionEntity): Boolean {
        val execution = signalEventSubscriptionEntity.execution
        return !execution.isEnded && !execution.isCanceled
    }

    private fun startProcessInstances(
        startSignalEventSubscriptions: List<EventSubscriptionEntity>,
        processDefinitions: Map<String, ProcessDefinitionEntity>
    ) {
        for (signalStartEventSubscription in startSignalEventSubscriptions) {
            val processDefinition = processDefinitions[signalStartEventSubscription.id]
            if (processDefinition != null) {
                val signalStartEvent = processDefinition.findActivity(signalStartEventSubscription.activityId)
                val processInstance = processDefinition.createProcessInstanceForInitial(signalStartEvent)
                if (processInstance is ExecutionEntity) {
                    processInstance.businessKey = businessKey
                }

                processInstance.start(startProcessVariables)
            }
        }
    }

    private fun filterIntermediateSubscriptions(
        subscriptions: List<EventSubscriptionEntity>
    ): List<EventSubscriptionEntity> {
        val result: MutableList<EventSubscriptionEntity> = ArrayList()
        for (subscription in subscriptions) {
            if (subscription.executionId != null) {
                result.add(subscription)
            }
        }
        return result
    }

    private fun filterStartSubscriptions(subscriptions: List<EventSubscriptionEntity>): List<EventSubscriptionEntity> {
        val result: MutableList<EventSubscriptionEntity> = ArrayList()
        for (subscription in subscriptions) {
            if (subscription.executionId == null) {
                result.add(subscription)
            }
        }
        return result
    }
}
