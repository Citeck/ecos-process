package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.interceptor.CommandContext
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.util.EnsureUtil
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_EVENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert.BpmnDataValue

class SignalEventByIdCmd(
    private val signalId: String,
    eventData: BpmnDataValue
) : Command<Unit> {

    private val eventVariable = mapOf(VAR_EVENT to eventData)

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
                processDefinitions[eventSubscription.id] = processDefinition
            }
        }
        return processDefinitions
    }

    private fun notifyExecutions(catchSignalEventSubscription: List<EventSubscriptionEntity>) {
        for (signalEventSubscriptionEntity in catchSignalEventSubscription) {
            if (isActiveEventSubscription(signalEventSubscriptionEntity)) {
                signalEventSubscriptionEntity.eventReceived(eventVariable, false)
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
                processInstance.start(eventVariable)
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
