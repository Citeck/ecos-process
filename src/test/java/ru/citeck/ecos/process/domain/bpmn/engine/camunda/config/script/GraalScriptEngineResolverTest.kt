package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import org.assertj.core.api.Assertions.assertThat
import org.graalvm.polyglot.Engine
import org.junit.jupiter.api.Test
import ru.citeck.ecos.webapp.lib.spring.context.script.EcosGraalJsProps
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

/**
 * The property this suite exists for is REUSE, and nothing in the platform asserted it before.
 *
 * Camunda asks the resolver for a script engine on every single script evaluation and never caches a GraalJS one
 * (`isCachable` reads `getParameter("THREADING")`, which GraalJS answers `null`). Before the fix that meant a new
 * polyglot [Engine] per evaluation, each registered forever in the static `Engine.ENGINES` set — measured on a
 * production heap dump as 4,937 engines holding 88 % of a 4 GB heap, and reproduced here in a few hundred calls.
 *
 * So the first test counts engines, and it fails loudly if anyone reintroduces a per-call `Engine.newBuilder()`.
 *
 * Every test drives the PUBLIC entry point Camunda itself calls — `getScriptEngine(language, resolveFromCache)`
 * with caching ON — so the `isCachable` decision is part of what is under test, not bypassed.
 */
class GraalScriptEngineResolverTest {

    private fun resolver(nashornCompat: Boolean = false) = GraalScriptEngineResolver(ScriptEngineManager(), EcosGraalJsProps(nashornCompat))

    /** Live entries of the static `Engine.ENGINES` registry — the exact structure that leaked. */
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

        assertThat(liveEngines())
            .`as`("200 resolutions must reuse ONE polyglot Engine; a per-call Engine.newBuilder() leaks them all")
            .isEqualTo(before)
    }

    @Test
    fun `each resolution still gets its own context, so globals never leak between scripts`() {
        val resolver = resolver()

        resolver.getScriptEngine("javascript", true).eval("var leakedGlobal = 42;")
        val second = resolver.getScriptEngine("javascript", true)

        assertThat(second.eval("typeof leakedGlobal"))
            .`as`("sharing the ENGINE must not share the CONTEXT — a script's globals stay in its own evaluation")
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
    fun `nashorn compatibility survives the engine being shared`() {
        // `js.nashorn-compat` is an EXPERIMENTAL ENGINE option, so sharing the engine is exactly where it could
        // be dropped. The marker is measured, not guessed: of the usual nashorn globals, `JSAdapter`, `__LINE__`
        // and `quit` are the ones this graal version really gates on the flag (`print`/`load`/`trimLeft` exist
        // either way, and `Java`/`importClass` need host access this context does not grant).
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
