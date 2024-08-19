package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class GraalScriptEngineResolver(
    scriptEngineManager: ScriptEngineManager,
    private val props: EcosGraalJsProps
) : DefaultScriptEngineResolver(scriptEngineManager) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getJavaScriptScriptEngine(language: String): ScriptEngine {

        val engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")

        if (props.nashornCompat) {
            log.info { "Enable nashorn compatibility mode" }
            engine.allowExperimentalOptions(true)
                .option("js.nashorn-compat", "true")
        }

        return GraalJSScriptEngine.create(engine.build(), Context.newBuilder("js"))
    }
}
