package ru.citeck.ecos.process.domain.bpmn.io

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.springframework.beans.factory.annotation.Value as ConfigValue

/**
 * Service for applying automatic layout to BPMN diagrams using the bpmn-auto-layout JavaScript library.
 * This service uses GraalVM to execute the JavaScript library within the JVM.
 *
 * A GraalJS [Context] is single-threaded, so each layout call borrows a context from a bounded pool.
 * All contexts share one [Engine], which lets the parsed bundle be reused instead of re-parsed per context,
 * so an extra context is cheap in memory. Layout is CPU-bound, so the pool size should not exceed available CPUs.
 *
 * A layout call that exceeds the timeout is stopped by cancelling its context from a watchdog thread.
 * A cancelled context is unusable, so it is dropped and the pool creates a fresh one on demand.
 */
@Component
class BpmnAutoLayoutService(
    @ConfigValue($$"${ecos-process.bpmn.auto-layout.max-contexts:2}")
    maxContexts: Int = 2
) {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val JS_BUNDLE_PATH = "/js/bpmn-auto-layout/bundle.umd.js"
        private const val LAYOUT_TIMEOUT_MS = 60_000L
    }

    private lateinit var engine: Engine
    private lateinit var bundleSource: Source

    private val maxContexts = maxContexts.coerceAtLeast(1)
    private val contextPermits = Semaphore(this.maxContexts)
    private val idleContexts = ConcurrentLinkedQueue<LayoutContext>()
    private val allContexts = ConcurrentLinkedQueue<LayoutContext>()

    private val watchdog = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, "bpmn-auto-layout-watchdog").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }

    // Visible for tests
    internal var layoutTimeoutMs = LAYOUT_TIMEOUT_MS

    @PostConstruct
    fun init() {
        try {
            log.info { "Initializing BpmnAutoLayoutService with bundled library" }

            engine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build()

            val jsResource = this::class.java.getResourceAsStream(JS_BUNDLE_PATH)
                ?: throw IllegalStateException("Could not find bpmn-auto-layout bundle at $JS_BUNDLE_PATH")

            bundleSource = jsResource.reader().use {
                Source.newBuilder("js", it, "bpmn-auto-layout-bundle").build()
            }

            // Create the first context eagerly to fail fast on a broken bundle
            idleContexts.add(createContext())

            log.info { "BpmnAutoLayoutService initialized successfully with bundled library, max contexts: $maxContexts" }
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize BpmnAutoLayoutService" }
            throw IllegalStateException("Failed to initialize BpmnAutoLayoutService", e)
        }
    }

    @PreDestroy
    fun destroy() {
        watchdog.shutdownNow()
        allContexts.forEach { runCatching { it.context.close() } }
        allContexts.clear()
        idleContexts.clear()
        if (::engine.isInitialized) {
            runCatching { engine.close() }
        }
    }

    private fun createContext(): LayoutContext {
        val context = Context.newBuilder("js")
            .engine(engine)
            .allowAllAccess(false)
            .build()
        try {
            context.eval(bundleSource)

            // Get the layoutProcess function from the global BpmnAutoLayout object (UMD exposes it this way)
            val bpmnAutoLayout = context.getBindings("js").getMember("BpmnAutoLayout")
                ?: throw IllegalStateException("BpmnAutoLayout global object not found in bundle")
            bpmnAutoLayout.getMember("layoutProcess")
                ?: throw IllegalStateException("layoutProcess function not found in BpmnAutoLayout object")

            // layoutProcess returns a Promise. GraalJS has no event loop: pending promise jobs run before
            // the call returns to Java, so the returned state object already holds the outcome.
            val layoutFunction = context.eval(
                "js",
                """
                (xml) => {
                    const state = {};
                    BpmnAutoLayout.layoutProcess(xml).then(
                        r => { state.value = r; },
                        e => { state.error = String(e); }
                    );
                    return state;
                }
                """
            )
            val layoutContext = LayoutContext(context, layoutFunction)
            allContexts.add(layoutContext)
            log.debug { "Created auto-layout JS context, total: ${allContexts.size}" }
            return layoutContext
        } catch (e: Exception) {
            context.close()
            throw e
        }
    }

    private fun <T> withContext(deadline: Long, action: (LayoutContext) -> T): T {
        val waitMs = (deadline - System.currentTimeMillis()).coerceAtLeast(0)
        if (!contextPermits.tryAcquire(waitMs, TimeUnit.MILLISECONDS)) {
            throw IllegalStateException("Timeout waiting for a free auto-layout JS context")
        }
        try {
            // Permits bound concurrent holders, so the pool never grows beyond maxContexts
            val layoutContext = idleContexts.poll() ?: createContext()
            val remainingMs = deadline - System.currentTimeMillis()
            if (remainingMs <= 0) {
                idleContexts.add(layoutContext)
                throw IllegalStateException("Timeout waiting for a free auto-layout JS context")
            }
            // Whoever flips this first decides the context's fate: the call returns it to the pool,
            // the watchdog cancels it. Future.cancel can't decide this, as it succeeds on a running task.
            val settled = AtomicBoolean(false)
            val watchdogTask = watchdog.schedule(
                {
                    if (settled.compareAndSet(false, true)) {
                        cancel(layoutContext)
                    }
                },
                remainingMs,
                TimeUnit.MILLISECONDS
            )
            try {
                return action(layoutContext)
            } catch (e: PolyglotException) {
                if (e.isCancelled) {
                    throw IllegalStateException("Layout process timed out after $layoutTimeoutMs ms", e)
                }
                throw e
            } finally {
                watchdogTask.cancel(false)
                if (settled.compareAndSet(false, true)) {
                    idleContexts.add(layoutContext)
                } else {
                    // The watchdog has cancelled (or is cancelling) this context, so it must not be reused
                    allContexts.remove(layoutContext)
                }
            }
        } finally {
            contextPermits.release()
        }
    }

    /**
     * High-level entry point used by transport adapters (REST controller and EcosWebExecutor).
     * Validates input and wraps the transformation result and any failure into a response envelope
     * with success flag, so callers can treat layout as a non-throwing operation.
     */
    fun applyAutoLayout(request: BpmnAutoLayoutRequest): BpmnAutoLayoutResponse {
        log.debug { "Received request to apply auto-layout to BPMN definition" }

        if (request.definition.isBlank()) {
            return BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "BPMN definition cannot be empty"
            )
        }

        if (!isValidBpmnXml(request.definition)) {
            return BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "Invalid BPMN XML format. Definition must be valid BPMN XML"
            )
        }

        return try {
            val transformedDefinition = applyAutoLayout(request.definition)

            log.debug { "Successfully applied auto-layout to BPMN definition" }

            BpmnAutoLayoutResponse(
                success = true,
                definition = transformedDefinition,
                message = "Layout applied successfully"
            )
        } catch (e: Exception) {
            log.error(e) { "Failed to apply auto-layout to BPMN definition" }

            BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "Failed to apply auto-layout: ${e.message}"
            )
        }
    }

    private fun isValidBpmnXml(definition: String): Boolean {
        val trimmed = definition.trim()
        return trimmed.startsWith("<?xml") ||
            trimmed.contains("<definitions") ||
            trimmed.contains("<bpmn:definitions")
    }

    /**
     * Applies automatic layout to a BPMN XML string.
     *
     * @param bpmnXml The BPMN XML string to apply layout to
     * @return The BPMN XML string with updated diagram layout
     * @throws IllegalStateException if the layout operation fails
     */
    fun applyAutoLayout(bpmnXml: String): String {
        return try {
            log.debug { "Applying auto-layout to BPMN XML" }

            // One deadline covers both waiting for a free context and the layout itself
            val deadline = System.currentTimeMillis() + layoutTimeoutMs

            withContext(deadline) { layoutContext ->
                val state = layoutContext.layoutFunction.execute(bpmnXml)
                when {
                    state.hasMember("error") -> throw IllegalStateException(
                        "Layout process failed: ${state.getMember("error").asString()}"
                    )
                    state.hasMember("value") -> state.getMember("value").asString()
                    // Can only happen if the library starts depending on timers or other host callbacks
                    else -> throw IllegalStateException("Layout process did not complete synchronously")
                }
            }
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            log.error(e) { "Failed to apply auto-layout to BPMN XML" }
            throw IllegalStateException("Failed to apply auto-layout to BPMN XML", e)
        }
    }

    fun isReady(): Boolean {
        return ::engine.isInitialized && allContexts.isNotEmpty()
    }

    private fun cancel(layoutContext: LayoutContext) {
        log.warn { "Auto-layout exceeded $layoutTimeoutMs ms, cancelling its JS context" }
        runCatching { layoutContext.context.close(true) }
            .onFailure { log.warn(it) { "Failed to cancel auto-layout JS context" } }
    }

    private class LayoutContext(
        val context: Context,
        val layoutFunction: Value
    )
}

data class BpmnAutoLayoutRequest(
    val definition: String
)

data class BpmnAutoLayoutResponse(
    val success: Boolean,
    val definition: String,
    val message: String
)
