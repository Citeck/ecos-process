package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.camunda.bpm.dmn.engine.impl.el.VariableContextScriptBindings
import org.camunda.bpm.engine.variable.Variables
import org.graalvm.polyglot.Engine
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.StringReader
import javax.script.Bindings
import javax.script.SimpleBindings
import org.graalvm.polyglot.Context as PolyglotContext

/**
 * Drives [EcosDmnScriptEngine] in the shape camunda's DMN uses: bindings wrapped in a variable context, then one
 * eval against them. The end-to-end suite proves the wiring, this proves the engine itself in milliseconds.
 */
class EcosDmnScriptEngineTest {

    private lateinit var polyglotEngine: Engine
    private lateinit var engine: EcosDmnScriptEngine

    @BeforeEach
    fun setUp() {
        polyglotEngine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build()
        engine = EcosDmnScriptEngine(GraalJSScriptEngine.create(polyglotEngine, PolyglotContext.newBuilder("js")))
    }

    @AfterEach
    fun tearDown() {
        polyglotEngine.close(true)
    }

    private fun dmnBindings(): Bindings {
        return VariableContextScriptBindings.wrap(
            engine.createBindings(),
            Variables.createVariables().putValue("color", "green").asVariableContext()
        )
    }

    @Test
    fun `createBindings must not be GraalJS's own, or the second context it builds could not be closed`() {
        assertThat(engine.createBindings())
            .`as`("GraalJSBindings would lazily build a context that VariableContextScriptBindings then hides")
            .isExactlyInstanceOf(SimpleBindings::class.java)
    }

    @Test
    fun `an evaluation closes the context it created`() {
        val bindings = dmnBindings()

        assertThat(engine.eval("color", bindings)).isEqualTo("green")

        val context = PolyglotResults.contextOf(bindings)
        assertThat(context).`as`("GraalJS must have cached its one context where this engine can find it").isNotNull
        assertThatThrownBy { context!!.eval("js", "1") }
            .`as`("the context must be closed by the time the evaluation returns")
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `a lazy result is detached, so it survives the close`() {
        // a js array is a PolyglotList bound to the context; without the conversion this throws on first read
        assertThat(engine.eval("[1, 2, 3]", dmnBindings())).isEqualTo(listOf(1, 2, 3))
        assertThat(engine.eval("({a: 1})", dmnBindings())).isEqualTo(mapOf("a" to 1))
        assertThat(engine.eval("new Date(0)", dmnBindings())).isEqualTo("1970-01-01T00:00:00Z")
    }

    @Test
    fun `a failing script still closes its context, and still reports its own failure`() {
        val bindings = dmnBindings()

        assertThatThrownBy { engine.eval("throw new Error('boom')", bindings) }
            .hasMessageContaining("boom")

        assertThatThrownBy { PolyglotResults.contextOf(bindings)!!.eval("js", "1") }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `many evaluations leave no context behind`() {
        val before = PolyglotContexts.liveCount(polyglotEngine)

        repeat(20) { i -> assertThat(engine.eval("$i + 1", dmnBindings())).isEqualTo(i + 1) }

        assertThat(PolyglotContexts.liveCount(polyglotEngine)).isEqualTo(before)
    }

    @Test
    fun `the evaluation paths this engine cannot close must fail loudly`() {
        // they evaluate against the engine's shared default context, which can be neither closed nor left open
        assertThatThrownBy { engine.eval("1") }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { engine.eval(StringReader("1")) }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { engine.eval("1", engine.context) }
            .isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { engine.eval(StringReader("1"), engine.context) }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun `the engine must not offer compilation, which would retain a context per compiled expression`() {
        // compile() parses on the engine's default context and nothing closes it, so the guard is to not offer it
        assertThat(engine).isNotInstanceOf(javax.script.Compilable::class.java)
    }
}
