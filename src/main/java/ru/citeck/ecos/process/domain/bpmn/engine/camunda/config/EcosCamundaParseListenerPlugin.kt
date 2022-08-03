package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog.listener.BpmnElementsLogParseListener
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.multiinstance.MultiInstanceUserTaskAssignParseListener

@Component
class EcosCamundaParseListenerPlugin(
    private val multiInstanceUserTaskAssignParseListener: MultiInstanceUserTaskAssignParseListener,
    private val bpmnElementsLogParseListener: BpmnElementsLogParseListener
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(multiInstanceUserTaskAssignParseListener)
        listeners.add(bpmnElementsLogParseListener)

        processEngineConfiguration.customPreBPMNParseListeners = listeners
    }
}
