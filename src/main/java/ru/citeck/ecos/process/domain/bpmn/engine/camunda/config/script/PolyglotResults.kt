package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import io.github.oshai.kotlinlogging.KotlinLogging
import org.graalvm.polyglot.Context
import ru.citeck.ecos.commons.utils.script.ScriptUtils
import java.time.Instant
import javax.script.Bindings

/**
 * Shared by the two places that own a polyglot context: [EcosScriptingEnvironment] for BPMN scripts and
 * [EcosDmnScriptEngine] for scripted decision tables. They have different context lifetimes and nothing else in
 * common, so the three helpers they both need live here.
 */
object PolyglotResults {

    private val log = KotlinLogging.logger {}

    /**
     * Where `GraalJSScriptEngine` caches its context inside the bindings. GraalJS internals, not public API.
     */
    const val POLYGLOT_CONTEXT = "polyglot.context"

    fun contextOf(bindings: Bindings): Context? {
        return bindings[POLYGLOT_CONTEXT] as? Context
    }

    /**
     * Converts [result] while [context] is still open, so closing it cannot invalidate the value. GraalJS returns
     * `eval(source).as(Object.class)`, which is lazy for everything but primitives: a js object is a `PolyglotMap`,
     * an array a `PolyglotList`, a `Date` a `PolyglotMap`.
     *
     * Going through [Context.asValue] instead of copying the already-mapped object matters, because it lets
     * [ScriptUtils.convertToJava] see a `Date` as an instant before it sees it as a map with no members.
     *
     * Inherited limits, none of them storable as a process variable anyway: a returned function becomes an empty
     * map, a cyclic result recurses, and a host `Map` or `Collection` comes back as a copy rather than the same
     * instance (host objects of other types are returned as themselves).
     */
    fun detach(context: Context, result: Any?): Any? {
        result ?: return null
        return isoDates(ScriptUtils.convertToJava(context.asValue(result)))
    }

    /**
     * Closes everything even if one of them fails, since a skipped close is the leak we are here to prevent.
     *
     * A close failure is logged and attached to [failure], never thrown on its own. `Context.close()` only throws
     * when the context runs on another thread or was cancelled by the guest, neither of which can happen once the
     * script has returned on this thread. If it happens anyway, one leaked context is cheaper than a failed
     * activity, or than a failed transaction: the BPMN close runs from a `TransactionListener`, and
     * `fireTransactionEvent` does not guard its listeners.
     */
    fun closeAll(closeables: List<AutoCloseable>, failure: Throwable?) {
        val closeFailures = closeables.mapNotNull { runCatching { it.close() }.exceptionOrNull() }
        closeFailures.forEach { closeFailure ->
            log.warn(closeFailure) { "Failed to close a polyglot context; it stays in the shared engine" }
            failure?.takeIf { it !== closeFailure }?.addSuppressed(closeFailure)
        }
    }

    /**
     * A js `Date` arrives as an [Instant], and ISO-8601 is what the platform already asks script authors to produce
     * (see `PolyglotMapSerializer`'s message). Nested dates included.
     *
     * DMN inherits this. Camunda's `DateDataTypeTransformer` wants no trailing `Z`, so a decision that needs a DMN
     * date should build it in FEEL; before this class such an output was an unusable `PolyglotMap` anyway.
     */
    private fun isoDates(value: Any?): Any? {
        return when (value) {
            is Instant -> value.toString()
            is List<*> -> value.map { isoDates(it) }
            is Map<*, *> -> value.entries.associateTo(LinkedHashMap()) { (k, v) -> k to isoDates(v) }
            else -> value
        }
    }
}
