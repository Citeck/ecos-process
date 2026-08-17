package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.EcosDmnScriptEngineResolver
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.EcosScriptingEnvironment
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
        initDmnScriptEngine(processEngineConfiguration)
    }

    /**
     * Has to be pre-set here rather than edited later: `initDmnEngine` builds the DMN engine during `init()` and
     * captures the resolver, and its builder only installs the default one when nothing is set yet. The delegate is
     * a lambda because `scriptingEngines` appears later, in `initScripting()`.
     */
    private fun initDmnScriptEngine(engineConfig: ProcessEngineConfigurationImpl) {
        val dmnConfig = engineConfig.dmnEngineConfiguration
            ?: DefaultDmnEngineConfiguration().also { engineConfig.dmnEngineConfiguration = it }
        dmnConfig.scriptEngineResolver = EcosDmnScriptEngineResolver { engineConfig.scriptingEngines }
    }

    private fun initHistoryProps(engineConfig: ProcessEngineConfigurationImpl) {
        engineConfig.historyTimeToLive = "P360D"
    }

    private fun initScriptEngine(engineConfig: ProcessEngineConfigurationImpl) {
        val props = ecosWebAppEnvironment.getValue(SCRIPT_PROPS_KEY, EcosGraalJsProps::class.java)
        engineConfig.setScriptEngineResolver(GraalScriptEngineResolver(ScriptEngineManager(), props))
    }

    /**
     * Not in [preInit]: the script factory, env resolvers and scripting engines this environment wraps are built by
     * [ProcessEngineConfigurationImpl.initScripting], which runs in between.
     */
    override fun postInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        processEngineConfiguration.scriptingEnvironment = EcosScriptingEnvironment(
            processEngineConfiguration.scriptFactory,
            processEngineConfiguration.envScriptResolvers,
            processEngineConfiguration.scriptingEngines
        )
    }
}
