package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.scripting.engine.DefaultScriptEngineResolver
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

/**
 * Resolves the GraalJS script engine camunda uses for BPMN `javascript` scripts.
 *
 * The polyglot [Engine] is shared and built once; only the [Context] is per-call. That split is the point of this
 * class. Camunda asks the resolver on every script evaluation and caches nothing for GraalJS, because
 * [DefaultScriptEngineResolver.isCachable] reads `getFactory().getParameter("THREADING")` and
 * `GraalJSEngineFactory` answers null for it. Building an `Engine` here per call therefore leaked one per
 * evaluation: `Engine` registers every instance in a static `ENGINES` set that holds the implementation strongly,
 * and entries leave only on close. A production heap dump had 4937 engines holding 88% of 4 GB.
 *
 * Sharing is safe. A polyglot `Engine` is designed to be shared by many contexts and is thread-safe, and isolation
 * is unchanged because the context is still per-call: a global defined in one is undefined in the next.
 * `ScriptExecutorImpl` already does the same elsewhere in the platform.
 *
 * Two things not to do here:
 *  - do not cache the ScriptEngine, that is the context, itself. A reused context carries top-level `var`
 *    declarations from one script into the next, which silently changes the meaning of any script testing
 *    `typeof x === 'undefined'`.
 *  - do not close a context here or in a delegating `eval`. A context lives as long as the bindings, which this
 *    class never sees: camunda evaluates the env scripts and the user's script against one bindings object, so
 *    closing at an eval boundary makes the next eval fail. [EcosScriptingEnvironment] owns that boundary, and
 *    something must, because a shared engine keeps every unclosed context alive for the life of the JVM.
 *
 * One consequence of sharing: all contexts on the engine must agree on host access. A context whose
 * `allowHostAccess` differs throws "Found different host access configuration for a context with a shared engine".
 * That holds today because camunda's `configureGraalJsScriptEngine` applies the same attributes on every
 * resolution from one process engine configuration.
 */
class GraalScriptEngineResolver(
    scriptEngineManager: ScriptEngineManager,
    private val props: EcosGraalJsProps
) : DefaultScriptEngineResolver(scriptEngineManager) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val sharedEngine: Engine by lazy { buildEngine() }

    override fun getJavaScriptScriptEngine(language: String): ScriptEngine {
        return GraalJSScriptEngine.create(sharedEngine, Context.newBuilder("js"))
    }

    private fun buildEngine(): Engine {
        val engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")

        if (props.nashornCompat) {
            log.info { "Enable nashorn compatibility mode" }
            engine.allowExperimentalOptions(true)
                .option("js.nashorn-compat", "true")
        }

        return engine.build()
    }
}
