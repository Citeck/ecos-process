package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.scripting.ScriptFactory
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory
import org.camunda.bpm.engine.impl.scripting.engine.ScriptBindingsFactory
import org.camunda.bpm.engine.impl.scripting.engine.ScriptingEngines
import org.camunda.bpm.engine.impl.scripting.env.ScriptEnvResolver
import org.camunda.bpm.engine.impl.scripting.env.ScriptingEnvironment
import org.graalvm.polyglot.Engine
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.script.ScriptEngineManager
import org.graalvm.polyglot.Context as PolyglotContext

/**
 * The property under test is that a script execution leaves no polyglot context behind.
 *
 * Counting engines is not enough: the first fix shared the polyglot `Engine` and a context leak walked straight
 * through that gate. So the count asserted here is the context count, and it is asserted both ways - the stock
 * [ScriptingEnvironment] is shown to leak two per execution, which proves the measurement can see the defect.
 *
 * Every test goes through [ScriptingEnvironment.execute], the entry point camunda itself calls, with an env script
 * and a user script sharing one bindings object as in production.
 */
class EcosScriptingEnvironmentTest {

    companion object {

        private const val JS = "javascript"
        private const val ENV_SCRIPT = "var envValue = 'from-env';"

        @JvmStatic
        fun resultShapes(): List<Arguments> {
            return listOf(
                Arguments.of("number", "1 + 1", 2),
                Arguments.of("string", "'abc'", "abc"),
                Arguments.of("boolean", "true", true),
                Arguments.of("null", "null", null),
                Arguments.of("undefined", "undefined", null),
                Arguments.of("jsObject", "({a: 1, b: 'x'})", mapOf("a" to 1, "b" to "x")),
                Arguments.of("jsArray", "[1, 2, 3]", listOf(1, 2, 3)),
                Arguments.of("nestedJsObject", "({a: [1, {b: 'x'}]})", mapOf("a" to listOf(1, mapOf("b" to "x")))),
                Arguments.of("jsDate", "new Date(0)", "1970-01-01T00:00:00Z"),
                Arguments.of(
                    "jsDateInsideObject",
                    "({when: new Date(0)})",
                    mapOf("when" to "1970-01-01T00:00:00Z")
                )
            )
        }
    }

    private lateinit var engineConfig: ProcessEngineConfigurationImpl
    private lateinit var scriptingEngines: ScriptingEngines

    /**
     * globals the 2-arg `execute` sees through a camunda [Resolver], as production variables do
     */
    private val globals = mutableMapOf<String, Any>()

    @BeforeEach
    fun setUp() {
        // camunda's scripting stack reads this thread local on every call; nothing is started, it is just config
        engineConfig = StandaloneInMemProcessEngineConfiguration()
        Context.setProcessEngineConfiguration(engineConfig)
        scriptingEngines = ScriptingEngines(
            ScriptBindingsFactory(listOf(ResolverFactory { globalsResolver() })),
            GraalScriptEngineResolver(ScriptEngineManager(), EcosGraalJsProps(false))
        )
    }

    private fun globalsResolver(): Resolver {
        return object : Resolver {

            override fun containsKey(key: Any?): Boolean {
                return globals.containsKey(key)
            }

            override fun get(key: Any?): Any? {
                return globals[key]
            }

            override fun keySet(): MutableSet<String> {
                return globals.keys
            }
        }
    }

    @AfterEach
    fun tearDown() {
        Context.removeProcessEngineConfiguration()
    }

    private fun envScriptResolvers(): List<ScriptEnvResolver> {
        return listOf(
            ScriptEnvResolver { language ->
                if (language == JS) arrayOf(ENV_SCRIPT) else null
            }
        )
    }

    private fun environment(): EcosScriptingEnvironment {
        return EcosScriptingEnvironment(ScriptFactory(), envScriptResolvers(), scriptingEngines)
    }

    private fun stockEnvironment(): ScriptingEnvironment {
        return ScriptingEnvironment(ScriptFactory(), envScriptResolvers(), scriptingEngines)
    }

    private fun run(env: ScriptingEnvironment, source: String): Any? {
        return env.execute(ScriptFactory().createScriptFromSource(JS, source), null)
    }

    private fun polyglotEngine(): Engine {
        return (scriptingEngines.getScriptEngineForLanguage(JS) as GraalJSScriptEngine).polyglotEngine
    }

    private fun liveContexts(): Int {
        return PolyglotContexts.liveCount(polyglotEngine())
    }

    @Test
    fun `the stock scripting environment leaks two polyglot contexts per execution`() {
        val stock = stockEnvironment()
        run(stock, "1")
        val before = liveContexts()

        repeat(20) { run(stock, "$it") }

        assertThat(liveContexts() - before)
            .`as`("this is the defect being guarded against; if it ever stops holding, the guard below is blind")
            .isEqualTo(40)
    }

    @Test
    fun `executing many scripts must not leave a single polyglot context behind`() {
        val env = environment()
        // one execution first, so the baseline already contains whatever a first resolution builds
        run(env, "1")
        val before = liveContexts()

        repeat(50) { i ->
            assertThat(run(env, "$i + 1")).isEqualTo(i + 1)
        }

        assertThat(liveContexts())
            .`as`("50 executions must close every context they create; the stock environment leaks 100 here")
            .isEqualTo(before)
    }

    @Test
    fun `the env script globals are still visible to the user script`() {
        // the reason a context cannot be closed per-eval: the env script and the user script share one context
        assertThat(run(environment(), "envValue"))
            .`as`("closing must not break the env script -> user script handover")
            .isEqualTo("from-env")
    }

    @Test
    fun `a script that throws still closes its contexts, and still reports its own failure`() {
        val env = environment()
        run(env, "1")
        val before = liveContexts()

        repeat(10) {
            val failure = runCatching { run(env, "throw new Error('boom')") }.exceptionOrNull()
            assertThat(failure).isNotNull()
            assertThat(generateSequence(failure) { it.cause }.mapNotNull { it.message }.joinToString())
                .`as`("the script's own error must reach the caller, not a close failure")
                .contains("boom")
        }

        assertThat(liveContexts()).`as`("a failing script must not leak its contexts either").isEqualTo(before)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("resultShapes")
    fun `a result survives the context being closed`(name: String, script: String, expected: Any?) {
        val result = run(environment(), script)

        assertThat(result)
            .`as`("%s must be detached from the polyglot context before it is closed", name)
            .isEqualTo(expected)
    }

    @Test
    fun `a host object passed through the script is returned unwrapped, not copied`() {
        // via the 2-arg execute, so the result really goes through the conversion
        val host = StringBuilder("host")
        globals["hostIn"] = host

        val result = run(environment(), "hostIn")

        assertThat(result).`as`("a host object must come back as itself").isSameAs(host)
    }

    @Test
    fun `the 4-arg execute must not close bindings it was given`() {
        val env = environment()
        val script = ScriptFactory().createScriptFromSource(JS, "1 + 1")
        val engine = scriptingEngines.getScriptEngineForLanguage(JS)
        val engineBindings = engine.createBindings()
        val bindings = scriptingEngines.scriptBindingsFactory.createBindings(null, engineBindings)

        assertThat(env.execute(script, null, bindings, engine)).isEqualTo(2)

        // observable, unlike asserting on the returned value: a closed context throws on any further use
        val evalContext = bindings[PolyglotResults.POLYGLOT_CONTEXT] as PolyglotContext
        assertThat(evalContext.eval("js", "2 + 2").asInt())
            .`as`("bindings passed in are the caller's; the 4-arg overload must leave their context open")
            .isEqualTo(4)

        evalContext.close()
        (engineBindings as? AutoCloseable)?.close()
    }

    @Test
    fun `concurrent executions stay correct and leave no context behind`() {
        val env = environment()
        run(env, "1")
        val before = liveContexts()
        val threads = 8
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val failure = AtomicReference<Throwable?>()

        repeat(threads) { id ->
            Thread({
                try {
                    Context.setProcessEngineConfiguration(engineConfig)
                    start.await()
                    repeat(25) { i ->
                        val x = id * 1000 + i
                        assertThat(run(env, "$x * 2")).isEqualTo(x * 2)
                    }
                } catch (e: Throwable) {
                    failure.compareAndSet(null, e)
                } finally {
                    Context.removeProcessEngineConfiguration()
                    done.countDown()
                }
            }, "ecos-scripting-env-$id").start()
        }
        start.countDown()

        assertThat(done.await(120, TimeUnit.SECONDS)).`as`("the concurrent executions must finish").isTrue()
        assertThat(failure.get()).`as`("closing a context must not disturb another thread's context").isNull()
        assertThat(liveContexts()).`as`("200 concurrent executions must leave nothing behind").isEqualTo(before)
    }
}
