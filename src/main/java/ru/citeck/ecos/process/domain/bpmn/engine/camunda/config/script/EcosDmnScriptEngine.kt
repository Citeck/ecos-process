package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import java.io.Reader
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.SimpleBindings

/**
 * Converts a scripted decision table's result while its polyglot context is open, then closes that context.
 * Installed by [EcosDmnScriptEngineResolver], which explains why this path needs its own closer.
 *
 * Deliberately not [javax.script.Compilable]. Camunda compiles when the engine says it can, and
 * `GraalJSScriptEngine.compile` parses on the engine's own default context, which nothing closes and the shared
 * engine keeps forever. That is a leaked context per compilation, and a compilation recurs on every redeploy and
 * every deployment-cache eviction, so it grows without bound. Without `Compilable` camunda calls
 * `eval(text, bindings)` instead, which this class closes; camunda does not compile ecmascript on the BPMN path
 * either, and Truffle shares parsed code across the contexts of one engine, so re-parsing costs little.
 */
class EcosDmnScriptEngine(private val delegate: GraalJSScriptEngine) : ScriptEngine by delegate {

    /**
     * Plain bindings on purpose. `GraalJSScriptEngine.createBindings()` returns `GraalJSBindings`, which builds a
     * second context as soon as anything is put into it, and camunda's `VariableContextScriptBindings` hides what
     * it wraps behind a protected field, so that context would be unreachable at eval time. With a plain map there
     * is exactly one context, cached under [PolyglotResults.POLYGLOT_CONTEXT] where [closeAfter] finds it.
     */
    override fun createBindings(): Bindings {
        return SimpleBindings()
    }

    override fun eval(script: String, bindings: Bindings): Any? {
        return closeAfter(bindings) {
            delegate.eval(script, bindings)
        }
    }

    override fun eval(reader: Reader, bindings: Bindings): Any? {
        return closeAfter(bindings) {
            delegate.eval(reader, bindings)
        }
    }

    // These evaluate against the engine's own ScriptContext, shared by every evaluation on this engine: closing it
    // would break the next caller, not closing it is the leak. Camunda's DMN never takes these paths.
    override fun eval(script: String): Any? {
        unsupported()
    }

    override fun eval(reader: Reader): Any? {
        unsupported()
    }

    override fun eval(script: String, context: ScriptContext): Any? {
        unsupported()
    }

    override fun eval(reader: Reader, context: ScriptContext): Any? {
        unsupported()
    }

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException(
            "EcosDmnScriptEngine only supports evaluation against caller-supplied bindings, because that is the " +
                "only shape whose polyglot context it can close. Pass bindings from createBindings(), as " +
                "camunda's DMN ExpressionEvaluationHandler does."
        )
    }

    private fun closeAfter(bindings: Bindings, eval: () -> Any?): Any? {
        var failure: Throwable? = null
        try {
            val result = eval()
            val context = PolyglotResults.contextOf(bindings) ?: return result
            return PolyglotResults.detach(context, result)
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            PolyglotResults.closeAll(listOfNotNull(PolyglotResults.contextOf(bindings)), failure)
        }
    }
}
