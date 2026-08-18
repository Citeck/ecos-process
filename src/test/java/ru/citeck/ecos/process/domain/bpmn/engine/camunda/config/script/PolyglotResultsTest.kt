package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.script.PolyglotResults.closeAll

/**
 * [PolyglotResults.closeAll] is the one piece of the polyglot lifecycle that must never fail its caller: the BPMN
 * close runs from a transaction listener and the DMN one from inside an evaluation, and neither guards it. It is
 * also the piece that must never give up half way, since a skipped close IS the COREDEV-418 leak.
 */
class PolyglotResultsTest {

    @Test
    fun `a close failure must not stop the remaining contexts from being closed`() {
        // if a close failure aborted the loop, the remaining context would stay in the shared engine
        val closed = mutableListOf<String>()
        val boom = IllegalStateException("close failed")
        val closeables = listOf(
            AutoCloseable {
                closed.add("first")
                throw boom
            },
            AutoCloseable { closed.add("second") }
        )

        val thrown = runCatching { closeAll(closeables, null) }.exceptionOrNull()

        assertThat(closed).`as`("both closeables must be attempted, in order").containsExactly("first", "second")
        assertThat(thrown)
            .`as`("cleanup runs from a transaction listener; fireTransactionEvent would fail the transaction")
            .isNull()
    }

    @Test
    fun `a close failure is suppressed onto the script's own failure, which still reaches the caller`() {
        val scriptFailure = RuntimeException("boom")
        val firstCloseFailure = IllegalStateException("first close failed")
        val secondCloseFailure = IllegalStateException("second close failed")
        val closeables = listOf(
            AutoCloseable { throw firstCloseFailure },
            AutoCloseable { throw secondCloseFailure }
        )

        closeAll(closeables, scriptFailure)

        assertThat(scriptFailure.suppressed)
            .`as`("every close failure must be attached to the script's failure, none of them swallowed")
            .containsExactly(firstCloseFailure, secondCloseFailure)
    }
}
