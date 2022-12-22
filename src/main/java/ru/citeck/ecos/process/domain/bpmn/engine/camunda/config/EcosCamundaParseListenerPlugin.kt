package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.listener.BpmnElementsEventsParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener.TaskVariablesParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskAssignParseListener

@Component
class EcosCamundaParseListenerPlugin(
    private val userTaskAssignParseListener: UserTaskAssignParseListener,
    private val bpmnElementsEventsParseListener: BpmnElementsEventsParseListener,
    private val taskParseListener: TaskVariablesParseListener
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(userTaskAssignParseListener)
        listeners.add(bpmnElementsEventsParseListener)
        listeners.add(taskParseListener)

        processEngineConfiguration.customPreBPMNParseListeners = listeners
    }
}
