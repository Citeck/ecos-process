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
 * Resolves the GraalJS script engine Camunda uses for BPMN `javascript` scripts.
 *
 * **The polyglot [Engine] is SHARED and built once; only the [Context] is per-call.** That split is the whole
 * point of this class and it must not be undone — the previous shape built a brand-new `Engine` on every
 * resolution and leaked every one of them:
 *
 *  - Camunda asks the resolver for an engine on EVERY script evaluation
 *    (`ScriptingEngines.getScriptEngineForLanguage` → `DefaultScriptEngineResolver.getScriptEngine(lang, true)`),
 *    and it caches the result only when [DefaultScriptEngineResolver.isCachable] holds — i.e. when
 *    `getFactory().getParameter("THREADING") != null`. `GraalJSEngineFactory.getParameter` answers `null` for
 *    every key it does not know, `THREADING` included, so **the result is never cached** and this method really
 *    is called once per evaluation.
 *  - `org.graalvm.polyglot.Engine` registers every instance in a static `ENGINES` set whose entry holds the
 *    engine IMPLEMENTATION strongly; entries leave only via `Engine.close()` or a reference-queue drain that
 *    runs from `contextClosed`. Nothing here closed either, so each engine — with its whole Truffle shape graph,
 *    language instances and instrumentation — stayed for the life of the JVM.
 *
 * Measured on a production heap dump (`citeck_eproc`, 2026-08-16): **4,937 leaked engines held 88.33 % of a
 * 4 GB heap** (3.78 GB of `com.oracle.truffle`), against 1.45 % for Camunda's own state. The app OOM-restarted
 * after roughly 5,000 script evaluations, twice in one day, and raising `-Xmx` only moved the failure later.
 * Reproduced outside the stand: 200 evaluations → 201 live engines; with the engine shared → 1, constant.
 *
 * **Why sharing is safe** (measured against graal 24.2.2, the version this app runs):
 *  - A polyglot `Engine` is explicitly designed to be shared by many `Context`s and is thread-safe; 8 threads
 *    evaluating concurrently through their own contexts on one shared engine returned correct results with no
 *    error.
 *  - Isolation is unchanged, because the CONTEXT is still per-call: a global defined in one context
 *    (`var leaked = 42`) is `undefined` in another context of the same engine. Nothing is reused that a script
 *    can observe.
 *  - Sharing is also what the rest of the platform does — `ru.citeck.ecos.commons.utils.script.ScriptExecutorImpl`
 *    keeps one lazily-built `Engine` per (language, key) behind exactly this double-checked lock and builds a
 *    fresh `Context` per evaluation. This class was the outlier.
 *
 * ⚠ Do not "improve" this by caching the ScriptEngine (i.e. the Context) itself: a reused context carries
 * top-level `var` declarations from one script into the next — measured — which silently changes the meaning of
 * every BPMN script that tests `typeof x === 'undefined'`.
 */
class GraalScriptEngineResolver(
    scriptEngineManager: ScriptEngineManager,
    private val props: EcosGraalJsProps
) : DefaultScriptEngineResolver(scriptEngineManager) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Volatile
    private var sharedEngine: Engine? = null
    private val engineLock = Any()

    override fun getJavaScriptScriptEngine(language: String): ScriptEngine {
        return GraalJSScriptEngine.create(getOrCreateEngine(), Context.newBuilder("js"))
    }

    /** The one shared engine, built on first use under a double-checked lock (the `ScriptExecutorImpl` shape). */
    private fun getOrCreateEngine(): Engine {
        return sharedEngine ?: synchronized(engineLock) {
            sharedEngine ?: buildEngine().also { sharedEngine = it }
        }
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
