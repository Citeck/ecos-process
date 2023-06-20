package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.events.CustomEventSubscriptionManager
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.mybatis.BpmnMyBatisExtendedSessionFactory
import javax.annotation.PostConstruct

private const val SELECT_TASKS_BY_IDS = "selectHistoricTaskInstanceByIds"
private const val SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES = "selectEventSubscriptionsByEventNames"
private const val SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES_LIKE_START = "selectEventSubscriptionsByEventNamesLikeStart"
private const val TRUNCATE_EVENT_SUBSCRIPTIONS = "truncateEventSubscriptions"
private const val SELECT_LATEST_DECISION_DEFINITIONS_BY_KEYS = "selectLatestDecisionDefinitionsByKeys"

@Component
class CamundaMyBatisExtension(
    private val processEngineConfiguration: ProcessEngineConfigurationImpl
) {

    private lateinit var factory: BpmnMyBatisExtendedSessionFactory

    @Autowired
    @Qualifier("camundaTransactionManager")
    private lateinit var transactionManager: PlatformTransactionManager

    @PostConstruct
    private fun init() {
        ext = this

        factory = BpmnMyBatisExtendedSessionFactory()
        factory.initFromProcessEngineConfiguration(processEngineConfiguration, transactionManager)
    }

    internal fun getHistoricTasksByIds(ids: List<String>): List<HistoricTaskInstance> {
        val command = Command {
            val params: Map<String, Any> = mapOf(
                "taskIds" to ids
            )

            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(SELECT_TASKS_BY_IDS, params) as List<HistoricTaskInstance>
        }

        return factory.commandExecutorTxRequired.execute(command)
    }

    internal fun getEventSubscriptionsByEventNames(eventNames: List<String>): List<EventSubscriptionEntity> {
        val command = Command {
            val params: Map<String, Any> = mapOf(
                "eventSubscriptionEventNames" to eventNames
            )

            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(
                SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES,
                params
            ) as List<EventSubscriptionEntity>
        }

        return factory.commandExecutorTxRequired.execute(command)
    }

    internal fun getEventSubscriptionsByEventNamesLikeStart(eventNames: List<String>): List<EventSubscriptionEntity> {
        val eventNamesWithLikeStartPattern = eventNames.map { "$it%" }

        val command = Command {
            val params: Map<String, Any> = mapOf(
                "eventSubscriptionEventNames" to eventNamesWithLikeStartPattern
            )

            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(
                SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES_LIKE_START,
                params
            ) as List<EventSubscriptionEntity>
        }

        val subscriptions = ArrayList(factory.commandExecutorTxRequired.execute(command))

        val commandContext = Context.getCommandContext()
        if (commandContext != null) {
            // Search subscriptions in command context.
            // selectList from DB doesn't return subscriptions which was created in current transaction
            val manager = commandContext.eventSubscriptionManager as? CustomEventSubscriptionManager
            val createdSubscriptions = manager?.getCreatedSubscriptions() ?: emptyList()
            for (subscription in createdSubscriptions) {
                val eventName = subscription.eventName ?: ""
                if (eventNames.any { name -> eventName.startsWith(name) }) {
                    subscriptions.add(subscription)
                }
            }
        }

        // remove duplicates
        val subscriptionsIds = mutableSetOf<String>()
        return subscriptions.filter {
            val id = it.id ?: ""
            id.isNotBlank() && subscriptionsIds.add(id)
        }
    }

    fun deleteAllEventSubscriptions() {
        val command = Command {
            it.dbSqlSession.sqlSession.update(TRUNCATE_EVENT_SUBSCRIPTIONS)
        }

        factory.commandExecutorTxRequired.execute(command)
    }

    fun getLatestDecisionDefinitionsByKeys(keys: List<String>): List<DecisionDefinitionEntity> {
        val command = Command {
            val params: Map<String, Any> = mapOf(
                "keys" to keys
            )

            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(
                SELECT_LATEST_DECISION_DEFINITIONS_BY_KEYS,
                params
            ) as List<DecisionDefinitionEntity>
        }

        return factory.commandExecutorTxRequired.execute(command)
    }
}

private lateinit var ext: CamundaMyBatisExtension

fun HistoryService.getHistoricTasksByIds(ids: List<String>): List<HistoricTaskInstance> {
    return ext.getHistoricTasksByIds(ids)
}

fun RuntimeService.getEventSubscriptionsByEventNames(eventNames: List<String>): List<EventSubscriptionEntity> {
    return ext.getEventSubscriptionsByEventNames(eventNames)
}

fun RuntimeService.getEventSubscriptionsByEventNamesLikeStart(eventNames: List<String>): List<EventSubscriptionEntity> {
    return ext.getEventSubscriptionsByEventNamesLikeStart(eventNames)
}

fun RepositoryService.getLatestDecisionDefinitionsByKeys(keys: List<String>): List<DecisionDefinitionEntity> {
    return ext.getLatestDecisionDefinitionsByKeys(keys)
}
