package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.cfg.TransactionContext
import org.camunda.bpm.engine.impl.cfg.TransactionListener
import org.camunda.bpm.engine.impl.cfg.TransactionState
import org.camunda.bpm.engine.impl.scripting.ExecutableScript
import org.camunda.bpm.engine.impl.scripting.ScriptFactory
import org.camunda.bpm.engine.impl.scripting.engine.ScriptingEngines
import org.camunda.bpm.engine.impl.scripting.env.ScriptEnvResolver
import org.camunda.bpm.engine.impl.scripting.env.ScriptingEnvironment
import org.graalvm.polyglot.Context
import javax.script.Bindings
import javax.script.SimpleBindings
import org.camunda.bpm.engine.impl.context.Context as CamundaContext

/**
 * Closes the GraalJS polyglot context a script execution creates, after converting the script's result out of it.
 * Nothing else closes it, and [GraalScriptEngineResolver]'s shared engine holds on to every context ever created.
 *
 * The context lives as long as the BINDINGS, which is why the close belongs here and not in the resolver:
 * [ScriptingEnvironment.execute] creates the bindings once, then evaluates both the env scripts (ecos_env.js,
 * which defines documentRef / document / log) and the user's script against them. Closing per eval breaks the
 * second one with "The Context is already closed".
 *
 * One context per execution, not two, because this class builds the bindings itself. Camunda would pass
 * `scriptEngine.createBindings()`, a `GraalJSBindings` that lazily builds a context of its own as soon as anything
 * is written to it, and the script never runs in that one. A plain [SimpleBindings] leaves only the context
 * `getOrCreateGraalJSBindings` builds for the eval, cached under [PolyglotResults.POLYGLOT_CONTEXT].
 *
 * The close is deferred to the end of the TRANSACTION. A script hands camunda guest values that outlive the call:
 * `execution.setVariable('arr', [1,2,3])` stores a `PolyglotList`, camunda keeps it as the variable's cached value
 * and re-serializes it later (`TypedValueField.isValuedImplicitlyUpdated`), so closing in a `finally` fails the
 * command. A `CommandContextListener` is not late enough either: `fireCommandContextClose` runs listeners in
 * registration order, and ours would be registered before any `TypedValueField` that starts caching the value
 * after the script ends. With no transaction context to register on, the close happens inline.
 *
 * That costs a peak rather than a leak: the eval contexts of one transaction all stay open until it commits, about
 * 85 KB each per thread. A script-heavy loop inside a single transaction is the shape to watch; an async boundary
 * bounds it per job.
 *
 * The result is still converted before the close, because camunda assigns a script task's `resultVariable` after
 * [execute] returns and would lose the same ordering race. See [PolyglotResults].
 *
 * Every BPMN script funnels through [ScriptingEnvironment.execute], so this covers all of them. A DMN table
 * declaring `expressionLanguage="javascript"` does not pass through here; [EcosDmnScriptEngine] handles that path.
 */
class EcosScriptingEnvironment(
    scriptFactory: ScriptFactory,
    scriptEnvResolvers: List<ScriptEnvResolver>,
    scriptingEngines: ScriptingEngines
) : ScriptingEnvironment(scriptFactory, scriptEnvResolvers, scriptingEngines) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun execute(script: ExecutableScript, scope: VariableScope?): Any? {

        val scriptEngine = scriptingEngines.getScriptEngineForLanguage(script.language)

        if (scriptEngine !is GraalJSScriptEngine) {
            val bindings = scriptingEngines.scriptBindingsFactory.createBindings(scope, scriptEngine.createBindings())
            return execute(script, scope, bindings, scriptEngine)
        }

        val bindings = scriptingEngines.scriptBindingsFactory.createBindings(scope, SimpleBindings())

        var failure: Throwable? = null
        try {
            return detachFromPolyglotContext(execute(script, scope, bindings, scriptEngine), bindings)
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            closeContexts(bindings, failure)
        }
    }

    /**
     * Only the final script's result needs this; [ScriptingEnvironment.execute] discards the env scripts'.
     */
    private fun detachFromPolyglotContext(result: Any?, bindings: Bindings): Any? {
        result ?: return null
        val context = evalContextOf(bindings) ?: return result
        return PolyglotResults.detach(context, result)
    }

    /**
     * Runs from a `finally`, so the read is wrapped: it must not throw over the script's own failure.
     */
    private fun closeContexts(bindings: Bindings, failure: Throwable?) {
        val deferred = listOfNotNull(runCatching { evalContextOf(bindings) }.getOrNull())
        if (deferred.isEmpty()) {
            return
        }
        val transactionContext = CamundaContext.getCommandContext()?.transactionContext
        if (transactionContext == null || !closeAfterTransaction(transactionContext, deferred)) {
            PolyglotResults.closeAll(deferred, failure)
        }
    }

    /**
     * Reports whether the close could be deferred at all, because a caller that cannot defer has to close inline.
     * One registered outcome is enough to count: closing inline after a partial registration would reintroduce the
     * bug, and one leaked context beats a context closed while camunda may still read from it.
     */
    private fun closeAfterTransaction(transactionContext: TransactionContext, deferred: List<AutoCloseable>): Boolean {
        val closer = TransactionListener { PolyglotResults.closeAll(deferred, null) }
        val onCommit = runCatching {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, closer)
        }.isSuccess
        val onRollback = runCatching {
            transactionContext.addTransactionListener(TransactionState.ROLLED_BACK, closer)
        }.isSuccess
        if (!onCommit || !onRollback) {
            log.warn { "Could not defer the polyglot context close to both transaction outcomes" }
        }
        return onCommit || onRollback
    }

    private fun evalContextOf(bindings: Bindings): Context? {
        return PolyglotResults.contextOf(bindings)
    }
}
