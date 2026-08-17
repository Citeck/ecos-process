package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import org.assertj.core.api.Assertions.assertThat
import org.graalvm.polyglot.Engine
import org.junit.jupiter.api.Test
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * The property under test is engine REUSE, which nothing asserted before.
 *
 * Camunda asks the resolver on every script evaluation and never caches a GraalJS engine, so a per-call
 * `Engine.newBuilder()` meant one leaked engine per evaluation. The first test counts engines and fails if anyone
 * brings that back.
 *
 * Every test goes through `getScriptEngine(language, resolveFromCache)` with caching on, so the `isCachable`
 * decision is part of what is tested rather than bypassed.
 */
class GraalScriptEngineResolverTest {

    private fun resolver(nashornCompat: Boolean = false): GraalScriptEngineResolver {
        return GraalScriptEngineResolver(ScriptEngineManager(), EcosGraalJsProps(nashornCompat))
    }

    /**
     * Live entries of the static `Engine.ENGINES` registry, the structure that leaked.
     */
    private fun liveEngines(): Int {
        val field = Engine::class.java.getDeclaredField("ENGINES")
        field.isAccessible = true
        val set = field.get(null) as MutableSet<*>
        return synchronized(set) { set.size }
    }

    @Test
    fun `resolving many script engines must not grow the polyglot engine registry`() {
        val resolver = resolver()
        // one resolution to build the shared engine, so the baseline already contains it
        resolver.getScriptEngine("javascript", true)
        val before = liveEngines()

        repeat(200) { i ->
            val engine = resolver.getScriptEngine("javascript", true)
            val bindings = SimpleBindings()
            bindings["x"] = i
            assertThat(engine.eval("x + 1", bindings)).isEqualTo(i + 1)
        }

        // Engine.ENGINES is JVM-global and pruned asynchronously, so it may legitimately drop below the baseline
        assertThat(liveEngines())
            .`as`("200 resolutions must reuse ONE polyglot Engine; a per-call Engine.newBuilder() leaks them all")
            .isLessThanOrEqualTo(before)
    }

    @Test
    fun `each resolution still gets its own context, so globals never leak between scripts`() {
        val resolver = resolver()

        resolver.getScriptEngine("javascript", true).eval("var leakedGlobal = 42;")
        val second = resolver.getScriptEngine("javascript", true)

        assertThat(second.eval("typeof leakedGlobal"))
            .`as`("sharing the ENGINE must not share the CONTEXT: globals stay in their own evaluation")
            .isEqualTo("undefined")
    }

    @Test
    fun `the shared engine serves concurrent resolutions correctly`() {
        val resolver = resolver()
        val threads = 8
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val failure = AtomicReference<Throwable?>()

        repeat(threads) { id ->
            Thread({
                try {
                    start.await()
                    repeat(50) { i ->
                        val engine = resolver.getScriptEngine("javascript", true)
                        val bindings = SimpleBindings()
                        bindings["x"] = id * 1000 + i
                        assertThat(engine.eval("x * 2", bindings)).isEqualTo((id * 1000 + i) * 2)
                    }
                } catch (e: Throwable) {
                    failure.compareAndSet(null, e)
                } finally {
                    done.countDown()
                }
            }, "graal-resolver-$id").start()
        }
        start.countDown()

        assertThat(done.await(120, TimeUnit.SECONDS)).`as`("the concurrent resolutions must finish").isTrue()
        assertThat(failure.get()).`as`("a shared polyglot Engine is thread-safe; contexts stay per-call").isNull()
    }

    @Test
    fun `a shared engine forces one host-access configuration on every context built from it`() {
        // The price of sharing: a resolution wanting different host access used to get its own engine, now it
        // fails, and it fails on first eval rather than on resolution. Here for the day camunda stops configuring
        // every resolution identically.
        val resolver = resolver()
        resolver.getScriptEngine("javascript", true).eval("1 + 1", SimpleBindings())

        val different = resolver.getScriptEngine("javascript", true)
        different.context.setAttribute("polyglot.js.allowHostAccess", true, ScriptContext.ENGINE_SCOPE)

        val failure = runCatching { different.eval("1 + 1", SimpleBindings()) }.exceptionOrNull()

        assertThat(failure)
            .`as`("a context whose host access differs from the shared engine's other contexts cannot be built")
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(failure).hasMessageContaining("Found different host access configuration")
    }

    @Test
    fun `nashorn compatibility survives the engine being shared`() {
        // js.nashorn-compat is an experimental ENGINE option, so sharing the engine is where it could get dropped.
        // JSAdapter and __LINE__ are the globals this graal version actually gates on the flag.
        val plain = resolver().getScriptEngine("javascript", true)
        assertThat(plain.eval("typeof JSAdapter")).isEqualTo("undefined")

        val nashorn = resolver(nashornCompat = true)
        repeat(2) {
            val engine = nashorn.getScriptEngine("javascript", true)
            assertThat(engine.eval("typeof JSAdapter"))
                .`as`("the engine option must hold on every resolution, not just the first")
                .isEqualTo("function")
            assertThat(engine.eval("typeof __LINE__")).isEqualTo("number")
        }
    }
}
