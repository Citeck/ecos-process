package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.interceptor.Command
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.mybatis.BpmnMyBatisExtendedSessionFactory
import javax.annotation.PostConstruct

private const val SELECT_TASKS_BY_IDS = "selectHistoricTaskInstanceByIds"

@Component
class CamundaMyBatisExtension(
    private val processEngineConfiguration: ProcessEngineConfigurationImpl
) {

    private lateinit var factory: BpmnMyBatisExtendedSessionFactory

    @PostConstruct
    private fun init() {
        ext = this

        factory = BpmnMyBatisExtendedSessionFactory()
        factory.initFromProcessEngineConfiguration(processEngineConfiguration)
    }

    internal fun getHistoricTasksByIds(ids: List<String>): List<HistoricTaskInstance> {
        val command = Command {
            val params: Map<String, Any> = mapOf(
                "taskIds" to ids
            )
            it.dbSqlSession.selectList(SELECT_TASKS_BY_IDS, params) as List<HistoricTaskInstance>
        }

        return factory.commandExecutorTxRequired.execute(command)
    }

}

private lateinit var ext: CamundaMyBatisExtension


fun HistoryService.getHistoricTasksByIds(ids: List<String>): List<HistoricTaskInstance> {
    return ext.getHistoricTasksByIds(ids)
}
