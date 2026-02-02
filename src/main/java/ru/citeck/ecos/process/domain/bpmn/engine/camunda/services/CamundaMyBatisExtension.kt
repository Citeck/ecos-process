package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import jakarta.annotation.PostConstruct
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.dmn.entity.repository.DecisionDefinitionEntity
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.events.CustomEventSubscriptionManager
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.mybatis.BpmnMyBatisExtendedSessionFactory
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SELECT_TASKS_BY_IDS = "selectHistoricTaskInstanceByIds"
private const val SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES = "selectEventSubscriptionsByEventNames"
private const val SELECT_EVENT_SUBSCRIPTIONS_BY_EVENT_NAMES_LIKE_START = "selectEventSubscriptionsByEventNamesLikeStart"
private const val SELECT_CONDITIONAL_EVENT_SUBSCRIPTIONS_BY_PROCESS_INSTANCE_IDS =
    "selectConditionalEventSubscriptionsByProcessInstanceIds"
private const val TRUNCATE_EVENT_SUBSCRIPTIONS = "truncateEventSubscriptions"

// DMN
private const val SELECT_LATEST_DECISION_DEFINITIONS_BY_KEYS = "selectLatestDecisionDefinitionsByKeys"
private const val SELECT_LATEST_DMN_DEFS_BY_WS_SYS_ID = "selectLatestDmnDefsByWsSysId"
private const val SELECT_COUNT_OF_LATEST_DMN_DEFS_BY_WS_SYS_ID = "selectCountOfLatestDmnDefsByWsSysId"

// BPMN
private const val SELECT_LATEST_PROCESS_DEFINITIONS_BY_KEYS = "selectLatestProcessDefinitionsByKeys"
private const val SELECT_LATEST_BPMN_DEFS_BY_WS_SYS_ID = "selectLatestBpmnDefsByWsSysId"
private const val SELECT_COUNT_OF_LATEST_BPMN_DEFS_BY_WS_SYS_ID = "selectCountOfLatestBpmnDefsByWsSysId"
private const val UPDATE_DOCUMENT_REF_VARIABLE = "updateDocumentRefVariables"
private const val UPDATE_DOCUMENT_REF_IN_BUSINESS_KEY = "updateDocumentRefInBusinessKey"
private const val UPDATE_DOCUMENT_REF_IN_EVENT_SUBSCRIPTIONS = "updateDocumentRefInEventSubscriptions"

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

        return aggregateEventSubscriptionsFromContext(subscriptions) { subscription ->
            val eventName = subscription.eventName ?: ""
            eventNames.any { name -> eventName.startsWith(name) }
        }
    }

    internal fun getConditionalEventSubscriptionsByProcessInstanceIds(processInstanceIds: List<String>): List<EventSubscriptionEntity> {
        if (processInstanceIds.isEmpty()) {
            return emptyList()
        }

        val command = Command {
            val params: Map<String, Any> = mapOf(
                "processInstanceIds" to processInstanceIds
            )

            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(
                SELECT_CONDITIONAL_EVENT_SUBSCRIPTIONS_BY_PROCESS_INSTANCE_IDS,
                params
            ) as List<EventSubscriptionEntity>
        }

        val subscriptions = factory.commandExecutorTxRequired.execute(command)

        return aggregateEventSubscriptionsFromContext(subscriptions) { subscription ->
            processInstanceIds.any { id -> id == subscription.processInstanceId }
        }
    }

    /**
     * SelectList from DB doesn't return subscriptions which was created in current transaction, so we need to
     * search subscriptions in command context.
     */
    private fun aggregateEventSubscriptionsFromContext(
        sourceList: List<EventSubscriptionEntity>,
        contextSubscriptionFilter: (EventSubscriptionEntity) -> Boolean
    ): List<EventSubscriptionEntity> {
        val commandContext = Context.getCommandContext() ?: return sourceList
        val aggregatedSubscriptions = sourceList.toMutableList()
        val manager = commandContext.eventSubscriptionManager as? CustomEventSubscriptionManager

        val contextSubscriptions = manager?.getCreatedSubscriptions() ?: emptyList()
        val filtered = contextSubscriptions.filter { contextSubscriptionFilter.invoke(it) }

        aggregatedSubscriptions.addAll(filtered)

        return aggregatedSubscriptions
            .filter {
                it.id != null && it.id.isNotBlank()
            }
            .distinct()
    }

    fun deleteAllEventSubscriptions() {
        val command = Command {
            it.dbSqlSession.sqlSession.update(TRUNCATE_EVENT_SUBSCRIPTIONS)
        }

        factory.commandExecutorTxRequired.execute(command)
    }

    private fun <T : Any> selectList(statementId: String, param: Any): List<T> {
        val command = Command {
            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectList(statementId, param) as List<T>
        }
        return factory.commandExecutorTxRequired.execute(command)
    }

    private fun selectCount(statementId: String, param: Any): Long {
        val command = Command {
            @Suppress("UNCHECKED_CAST")
            it.dbSqlSession.selectOne(statementId, param) as Long
        }
        return factory.commandExecutorTxRequired.execute(command)
    }

    fun getLatestDecisionDefinitionsByKeys(keys: List<String>): List<DecisionDefinitionEntity> {
        return selectList(
            SELECT_LATEST_DECISION_DEFINITIONS_BY_KEYS,
            mapOf("keys" to keys)
        )
    }

    fun getCountOfLatestDmnDefsByWsSysId(workspacesSysId: List<String>): Long {
        return selectCount(
            SELECT_COUNT_OF_LATEST_DMN_DEFS_BY_WS_SYS_ID,
            mapOf("workspacesSysId" to workspacesSysId)
        )
    }

    fun getLatestDmnDefsByWorkspacesSysId(
        workspacesSysId: List<String>,
        skipCount: Int,
        maxItems: Int
    ): List<DecisionDefinitionEntity> {
        return selectList(
            SELECT_LATEST_DMN_DEFS_BY_WS_SYS_ID,
            mapOf(
                "workspacesSysId" to workspacesSysId,
                "skipCount" to skipCount,
                "maxItems" to maxItems
            )
        )
    }

    fun getCountOfLatestBpmnDefsByWsSysId(
        workspacesSysId: List<String>,
        definitionKeyLike: String?
    ): Long {
        return selectCount(
            SELECT_COUNT_OF_LATEST_BPMN_DEFS_BY_WS_SYS_ID,
            mapOf(
                "keyLike" to definitionKeyLike,
                "workspacesSysId" to workspacesSysId
            )
        )
    }

    fun getLatestBpmnDefsByWorkspacesSysId(
        workspacesSysId: List<String>,
        definitionKeyLike: String?,
        skipCount: Int,
        maxItems: Int
    ): List<ProcessDefinitionEntity> {
        return selectList(
            SELECT_LATEST_BPMN_DEFS_BY_WS_SYS_ID,
            mapOf(
                "workspacesSysId" to workspacesSysId,
                "keyLike" to definitionKeyLike,
                "skipCount" to skipCount,
                "maxItems" to maxItems
            )
        )
    }

    /**
     * Updates all runtime variables in all active process instances
     * by replacing [oldRef] with [newRef] via direct SQL UPDATE on `ACT_RU_VARIABLE`.
     *
     * @return number of updated variable rows
     */
    fun updateDocumentRefVariables(oldRef: EntityRef, newRef: EntityRef): Int {
        val params = mapOf("oldRef" to oldRef.toString(), "newRef" to newRef.toString())
        val command = Command {
            val session = it.dbSqlSession.sqlSession
            session.update(UPDATE_DOCUMENT_REF_VARIABLE, params)
            val batchResults = session.flushStatements()
            batchResults.sumOf { result -> result.updateCounts.sum() }
        }
        return factory.commandExecutorTxRequired.execute(command)
    }

    /**
     * Updates the business key in `ACT_RU_EXECUTION` for all active process instances
     * where the current business key matches [oldRef], replacing it with [newRef].
     *
     * @return number of updated execution rows
     */
    fun updateDocumentRefInBusinessKey(oldRef: EntityRef, newRef: EntityRef): Int {
        val params = mapOf("oldRef" to oldRef.toString(), "newRef" to newRef.toString())
        val command = Command {
            val session = it.dbSqlSession.sqlSession
            session.update(UPDATE_DOCUMENT_REF_IN_BUSINESS_KEY, params)
            val batchResults = session.flushStatements()
            batchResults.sumOf { result -> result.updateCounts.sum() }
        }
        return factory.commandExecutorTxRequired.execute(command)
    }

    /**
     * Updates signal event subscription names in `ACT_RU_EVENT_SUBSCR` by replacing [oldRef]
     * with [newRef] via direct SQL UPDATE. Signal names follow the pattern
     * `TYPE;documentRef;ANY;hash`, so the replacement is scoped to occurrences
     * delimited by semicolons to avoid partial matches.
     *
     * @return number of updated event subscription rows
     */
    fun updateDocumentRefInEventSubscriptions(oldRef: EntityRef, newRef: EntityRef): Int {
        val params = mapOf("oldRef" to oldRef.toString(), "newRef" to newRef.toString())
        val command = Command {
            val session = it.dbSqlSession.sqlSession
            session.update(UPDATE_DOCUMENT_REF_IN_EVENT_SUBSCRIPTIONS, params)
            val batchResults = session.flushStatements()
            batchResults.sumOf { result -> result.updateCounts.sum() }
        }
        return factory.commandExecutorTxRequired.execute(command)
    }

    fun getLatestProcessDefinitionsByKeys(keys: List<String>): List<ProcessDefinitionEntity> {
        return selectList(
            SELECT_LATEST_PROCESS_DEFINITIONS_BY_KEYS,
            mapOf("keys" to keys)
        )
    }
}

fun HistoryService.getHistoricTasksByIds(
    ids: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<HistoricTaskInstance> {
    return camundaMyBatisExtension.getHistoricTasksByIds(ids)
}

fun RuntimeService.getEventSubscriptionsByEventNames(
    eventNames: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<EventSubscriptionEntity> {
    return camundaMyBatisExtension.getEventSubscriptionsByEventNames(eventNames)
}

fun RuntimeService.getEventSubscriptionsByEventNamesLikeStart(
    eventNames: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<EventSubscriptionEntity> {
    return camundaMyBatisExtension.getEventSubscriptionsByEventNamesLikeStart(eventNames)
}

fun RuntimeService.getConditionalEventSubscriptionsByProcessInstanceIds(
    processInstanceIds: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<EventSubscriptionEntity> {
    return camundaMyBatisExtension.getConditionalEventSubscriptionsByProcessInstanceIds(processInstanceIds)
}

fun RepositoryService.getLatestDecisionDefinitionsByKeys(
    keys: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<DecisionDefinitionEntity> {
    return camundaMyBatisExtension.getLatestDecisionDefinitionsByKeys(keys)
}

fun RepositoryService.getLatestProcessDefinitionsByKeys(
    keys: List<String>,
    camundaMyBatisExtension: CamundaMyBatisExtension
): List<ProcessDefinitionEntity> {
    return camundaMyBatisExtension.getLatestProcessDefinitionsByKeys(keys)
}
