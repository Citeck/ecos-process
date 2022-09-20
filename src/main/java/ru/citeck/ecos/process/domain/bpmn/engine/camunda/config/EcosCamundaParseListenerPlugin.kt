package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.listener.BpmnElementsLogParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.listener.TaskTitleParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask.UserTaskAssignParseListener

@Component
class EcosCamundaParseListenerPlugin(
    private val userTaskAssignParseListener: UserTaskAssignParseListener,
    private val bpmnElementsLogParseListener: BpmnElementsLogParseListener,
    private val taskTitleParseListener: TaskTitleParseListener
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(userTaskAssignParseListener)
        listeners.add(bpmnElementsLogParseListener)
        listeners.add(taskTitleParseListener)

        processEngineConfiguration.customPreBPMNParseListeners = listeners
    }
}
