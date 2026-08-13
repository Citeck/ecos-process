package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask.AiTaskParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener.BpmnElementsEventsParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.script.ScriptTaskParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener.TaskVariablesParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskAssignParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskAttsSyncParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskLazyApprovalParseListener

@Component
class EcosCamundaParseListenerPlugin(
    private val userTaskAssignParseListener: UserTaskAssignParseListener,
    private val userTaskLazyApprovalParseListener: UserTaskLazyApprovalParseListener,
    private val bpmnElementsEventsParseListener: BpmnElementsEventsParseListener,
    private val taskParseListener: TaskVariablesParseListener,
    private val taskAttsSyncParseListener: UserTaskAttsSyncParseListener,
    private val aiTaskParseListener: AiTaskParseListener,
    private val scriptTaskParseListener: ScriptTaskParseListener
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(userTaskAssignParseListener)
        listeners.add(userTaskLazyApprovalParseListener)
        listeners.add(bpmnElementsEventsParseListener)
        listeners.add(taskParseListener)
        listeners.add(taskAttsSyncParseListener)
        listeners.add(aiTaskParseListener)
        listeners.add(scriptTaskParseListener)

        processEngineConfiguration.customPreBPMNParseListeners = listeners
    }
}
