package ru.citeck.ecos.process.config.camunda

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component

//TODO: remove
@Component
class ParseListenerPlugin(
    private val testParseListener: TestParseListener
) : AbstractProcessEnginePlugin() {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl?) {
        println("===== CALL PRE INIT")

        if (processEngineConfiguration == null) return

        val listeners = processEngineConfiguration.customPreBPMNParseListeners ?: mutableListOf()

        listeners.add(testParseListener)

        processEngineConfiguration.customPreBPMNParseListeners = listeners

        println("===== END PRE INIT")
    }
}
