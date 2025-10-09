package ru.citeck.ecos.process.domain.bpmn.io

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.springframework.stereotype.Component

/**
 * Service for applying automatic layout to BPMN diagrams using the bpmn-auto-layout JavaScript library.
 * This service uses GraalVM to execute the JavaScript library within the JVM.
 */
@Component
class BpmnAutoLayoutService {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val JS_BUNDLE_PATH = "/js/bpmn-auto-layout/bundle.umd.js"
    }

    private lateinit var context: Context
    private lateinit var layoutFunction: Value

    @PostConstruct
    fun init() {
        try {
            log.info { "Initializing BpmnAutoLayoutService with bundled library" }

            context = Context.newBuilder("js")
                .allowAllAccess(false)
                .option("engine.WarnInterpreterOnly", "false")
                .build()

            val jsResource = this::class.java.getResourceAsStream(JS_BUNDLE_PATH)
                ?: throw IllegalStateException("Could not find bpmn-auto-layout bundle at $JS_BUNDLE_PATH")

            val source = Source.newBuilder("js", jsResource.reader(), "bpmn-auto-layout-bundle")
                .build()

            context.eval(source)

            // Get the layoutProcess function from the global BpmnAutoLayout object (UMD exposes it this way)
            val bpmnAutoLayout = context.getBindings("js").getMember("BpmnAutoLayout")
                ?: throw IllegalStateException("BpmnAutoLayout global object not found in bundle")

            layoutFunction = bpmnAutoLayout.getMember("layoutProcess")
                ?: throw IllegalStateException("layoutProcess function not found in BpmnAutoLayout object")

            log.info { "BpmnAutoLayoutService initialized successfully with bundled library" }
        } catch (e: Exception) {
            log.error(e) { "Failed to initialize BpmnAutoLayoutService" }
            throw IllegalStateException("Failed to initialize BpmnAutoLayoutService", e)
        }
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

            // Execute the layoutProcess function with the BPMN XML
            // The function returns a Promise, so we need to handle it
            val promiseScript = """
                (async function() {
                    const result = await layoutProcess(arguments[0]);
                    return result;
                })
            """

            val asyncFunction = context.eval("js", promiseScript)
            val promise = asyncFunction.execute(bpmnXml)

            // Wait for the promise to resolve
            val resultHandler = context.eval("js", "(promise) => { let result; " +
                "promise.then(r => result = r); return () => result; }")
            val getResult = resultHandler.execute(promise)

            // Poll for result (with timeout)
            var result: String? = null
            val startTime = System.currentTimeMillis()
            val timeout = 30000L // 30 seconds timeout

            while (result == null && (System.currentTimeMillis() - startTime) < timeout) {
                val res = getResult.execute()
                if (!res.isNull) {
                    result = res.asString()
                } else {
                    Thread.sleep(10)
                }
            }

            result ?: throw IllegalStateException("Timeout waiting for layout process to complete")
        } catch (e: Exception) {
            log.error(e) { "Failed to apply auto-layout to BPMN XML" }
            throw IllegalStateException("Failed to apply auto-layout to BPMN XML", e)
        }
    }

    fun isReady(): Boolean {
        return ::context.isInitialized && ::layoutFunction.isInitialized
    }
}
