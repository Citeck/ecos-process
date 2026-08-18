package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.ProcessEngineImpl
import org.graalvm.polyglot.Engine

/**
 * Counts the live entries of `PolyglotEngineImpl.contexts`, the strong set the leak grew in and the only place
 * "a script execution leaves no context behind" is observable.
 *
 * Reflective because the set is graal-internal: `Engine.receiver` is the `PolyglotEngineImpl` and its `contexts` is
 * an `EconomicSet` whose `size()` sits on a non-exported class. Kept here so a graal rename breaks in one place.
 */
object PolyglotContexts {

    /**
     * the shared engine a booted process engine resolves javascript on
     */
    fun liveCount(processEngine: ProcessEngine): Int {
        val resolver = (processEngine as ProcessEngineImpl).processEngineConfiguration.scriptEngineResolver
        val scriptEngine = resolver.getScriptEngine("javascript", true) as GraalJSScriptEngine
        return liveCount(scriptEngine.polyglotEngine)
    }

    fun liveCount(engine: Engine): Int {
        val impl = Engine::class.java.getDeclaredField("receiver")
            .apply { isAccessible = true }
            .get(engine)
        val contexts = impl.javaClass.getDeclaredField("contexts")
            .apply { isAccessible = true }
            .get(impl)
        val size = contexts.javaClass.methods.first { it.name == "size" && it.parameterCount == 0 }
        size.isAccessible = true
        return synchronized(impl) { size.invoke(contexts) as Int }
    }
}
