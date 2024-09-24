package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.GraalScriptEngineResolver
import ru.citeck.ecos.webapp.lib.env.EcosWebAppEnvironment
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import javax.script.ScriptEngineManager

class EcosCamundaEnginePlugin(
    private val ecosWebAppEnvironment: EcosWebAppEnvironment
) : AbstractProcessEnginePlugin() {

    companion object {
        const val SCRIPT_PROPS_KEY = "ecos.webapp.scripts.graaljs.camunda"
    }

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        initHistoryProps(processEngineConfiguration)
        initScriptEngine(processEngineConfiguration)
    }

    private fun initHistoryProps(engineConfig: ProcessEngineConfigurationImpl) {
        engineConfig.historyTimeToLive = "P360D"
    }

    private fun initScriptEngine(engineConfig: ProcessEngineConfigurationImpl) {
        val props = ecosWebAppEnvironment.getValue(SCRIPT_PROPS_KEY, EcosGraalJsProps::class.java)
        engineConfig.setScriptEngineResolver(GraalScriptEngineResolver(ScriptEngineManager(), props))
    }
}
