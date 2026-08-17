package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.scripting.env.ScriptEnvResolver
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.EcosDmnScriptEngineResolver
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.EcosScriptingEnvironment
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.GraalScriptEngineResolver
import ru.citeck.ecos.webapp.lib.env.EcosWebAppEnvironment
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import java.time.Duration

/**
 * Nothing in the leak fix is reachable from production unless this plugin installs it, and the order matters: the
 * environment has to go in `postInit`, because `initScripting` builds the objects it wraps and installs the stock
 * one, while the DMN resolver has to go in `preInit`, before the DMN engine captures it.
 *
 * So the test drives the hooks in the same order camunda's `init()` does: preInit, initScripting, postInit.
 * Building a real process engine would need a database this module's test classpath does not have.
 */
class EcosCamundaEnginePluginTest {

    companion object {
        private const val JS = "javascript"
        private const val MARKER_NAME = "markerFromEnvScript"
        private const val MARKER = "contributed-by-an-env-script-resolver"
    }

    /**
     * [StandaloneInMemProcessEngineConfiguration] with camunda's own `initScripting` exposed, so the plugin's
     * two hooks can be driven around it in the real order without booting an engine.
     */
    private class ScriptingConfiguration : StandaloneInMemProcessEngineConfiguration() {
        public override fun initScripting() {
            super.initScripting()
        }
    }

    private val environment = object : EcosWebAppEnvironment {
        override fun getMapPropertyKeys(prefix: String): Set<String> {
            return emptySet()
        }

        override fun hasValue(key: String): Boolean {
            return key == EcosCamundaEnginePlugin.SCRIPT_PROPS_KEY
        }

        override fun <T : Any> getValue(keys: List<String>, type: Class<T>): T {
            return getValue(keys.first(), type)
        }

        override fun getBoolean(key: String, default: Boolean): Boolean {
            return default
        }

        override fun getText(key: String, default: String): String {
            return default
        }

        override fun getDuration(key: String, default: Duration): Duration {
            return default
        }

        override fun getActiveProfiles(): List<String> {
            return emptyList()
        }

        override fun acceptsProfiles(vararg profiles: String): Boolean {
            return false
        }

        override fun <T : Any> getValue(key: String, type: Class<T>): T {
            check(key == EcosCamundaEnginePlugin.SCRIPT_PROPS_KEY) { "unexpected property requested: $key" }
            @Suppress("UNCHECKED_CAST")
            return EcosGraalJsProps(false) as T
        }
    }

    private fun configure(): ScriptingConfiguration {
        val config = ScriptingConfiguration()
        // camunda's BeansResolver reads this on every bindings lookup and initBeans, which is part of the full
        // init this test deliberately does not run, is what normally gives it an empty map
        config.beans = mutableMapOf()
        // stands in for the resolver that contributes ecos_env.js to every BPMN script
        config.envScriptResolvers = mutableListOf(
            ScriptEnvResolver { language -> if (language == JS) arrayOf("var $MARKER_NAME = '$MARKER';") else null }
        )
        val plugin = EcosCamundaEnginePlugin(environment)

        plugin.preInit(config)
        config.initScripting()
        plugin.postInit(config)

        return config
    }

    @Test
    fun `the plugin installs the leak-free scripting stack on the engine configuration`() {
        val config = configure()

        assertThat(config.scriptEngineResolver)
            .`as`("without this resolver every script evaluation builds - and leaks - its own polyglot Engine")
            .isInstanceOf(GraalScriptEngineResolver::class.java)
        assertThat(config.scriptingEnvironment)
            .`as`("postInit must win over the stock ScriptingEnvironment initScripting installs, or nothing closes the contexts")
            .isInstanceOf(EcosScriptingEnvironment::class.java)
        assertThat(config.dmnEngineConfiguration.scriptEngineResolver)
            .`as`("without this a javascript decision table leaks its polyglot contexts")
            .isInstanceOf(EcosDmnScriptEngineResolver::class.java)
    }

    @Test
    fun `the installed environment runs scripts through the configuration's own scripting objects`() {
        // behavioural rather than by field identity: all three constructor arguments matter, and a type check
        // would only see one of them
        val config = configure()
        Context.setProcessEngineConfiguration(config)
        try {
            val script = config.scriptFactory.createScriptFromSource(JS, MARKER_NAME)

            assertThat(config.scriptingEnvironment.execute(script, null))
                .`as`("the env scripts the configuration carries must reach the user's script")
                .isEqualTo(MARKER)
        } finally {
            Context.removeProcessEngineConfiguration()
        }
    }
}
