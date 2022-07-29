package ru.citeck.ecos.process.config.camunda

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.config.camunda.multiinstance.MultiInstanceUserTaskAssignPlugin

@Component
class EcosCamundaParseListenerPlugin(
    private val multiInstanceUserTaskAssignPlugin: MultiInstanceUserTaskAssignPlugin
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(multiInstanceUserTaskAssignPlugin)

        processEngineConfiguration.customPreBPMNParseListeners = listeners
    }
}
