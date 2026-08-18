package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import org.camunda.bpm.dmn.engine.impl.spi.el.DmnScriptEngineResolver
import javax.script.ScriptEngine

/**
 * Gives camunda's DMN engine a GraalJS script engine that closes the polyglot context each evaluation creates.
 *
 * A decision table declaring `expressionLanguage="javascript"` never passes through [EcosScriptingEnvironment]:
 * camunda's `ExpressionEvaluationHandler` resolves a `ScriptEngine` itself and builds its own bindings. Without
 * this, each evaluation left its contexts in the shared engine forever; 10 evaluations of a two-expression table
 * leaked 46 of them.
 *
 * Closing at the eval boundary is safe here and nowhere else: DMN wraps fresh bindings per expression and
 * evaluates exactly one script against them, so the eval and the bindings end together. BPMN shares one bindings
 * object between the env scripts and the user script, which is why it needs a later boundary.
 *
 * Installed by pre-setting the DMN engine configuration's resolver, because
 * `DmnEngineConfigurationBuilder` only installs the process engine's own `ScriptingEngines` when nothing is set
 * yet. The delegate is a lambda since `scriptingEngines` does not exist at `preInit` time.
 */
class EcosDmnScriptEngineResolver(
    private val delegate: () -> DmnScriptEngineResolver
) : DmnScriptEngineResolver {

    override fun getScriptEngineForLanguage(language: String?): ScriptEngine? {
        val scriptEngine = delegate().getScriptEngineForLanguage(language) ?: return null
        return if (scriptEngine is GraalJSScriptEngine) EcosDmnScriptEngine(scriptEngine) else scriptEngine
    }
}
