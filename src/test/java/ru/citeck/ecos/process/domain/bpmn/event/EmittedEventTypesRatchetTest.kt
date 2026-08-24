package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.classreading.CachingMetadataReaderFactory
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_ACTIVITY_ELEMENT_END
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_ACTIVITY_ELEMENT_START
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_FLOW_ELEMENT_TAKE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import java.lang.reflect.Modifier

/**
 * Set equality between the events the platform publishes and the vocabulary [EcosEventType] declares.
 *
 * Both families are enumerated from their source — the `BPMN_EVENT_*` constants next to `BpmnEventEmitter`,
 * and the `TYPE` constants of `ru.citeck.ecos.events2.type` — and compared with what the enum declares plus
 * the explicit exceptions below. Equality holds in both directions: a newly published event fails until it
 * is declared or excepted, and a stale exception fails too.
 */
class EmittedEventTypesRatchetTest {

    /**
     * Not ecos-events2 event types. `BpmnEventEmitter` builds an emitter for six names only; these three
     * are `eventType` tags of a `BpmnElementProcessingRequest` on the internal `bpmn-elements` queue
     * (`BpmnActivityExecutionListeners` → `BpmnElementsCreator`). They never reach the event bus, so a
     * subscription to them could not fire.
     */
    private val bpmnNamesThatAreQueueTagsNotEvents = setOf(
        BPMN_EVENT_ACTIVITY_ELEMENT_START,
        BPMN_EVENT_ACTIVITY_ELEMENT_END,
        BPMN_EVENT_FLOW_ELEMENT_TAKE
    )

    /**
     * Record events not offered to a BPMN process: `record-ref-changed` and `record-type-changed` re-shape a
     * record's identity at the storage level rather than marking a business moment.
     *
     * `record-parent-changed` is absent because the pinned ecos-events2 does not declare it; a dependency
     * bump that adds it will fail this test.
     */
    private val recordEventsDeliberatelyNotSubscribable = setOf(
        "record-ref-changed",
        "record-type-changed"
    )

    @Test
    fun `every bpmn event the emitter publishes is either declared or excepted`() {
        val emitted = constantsOf(
            "ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitterKt",
            namePrefix = "BPMN_EVENT_"
        )
        assertThat(emitted)
            .describedAs("BPMN_EVENT_* constants must be readable by reflection for the ratchet to mean anything")
            .isNotEmpty

        val declared = declaredEventNames().filter { it.startsWith("bpmn-") }.toSet()

        assertThat(declared + bpmnNamesThatAreQueueTagsNotEvents)
            .describedAs(
                "EcosEventType must declare every bpmn event on the bus, or except it explicitly. " +
                    "Declared: $declared, excepted: $bpmnNamesThatAreQueueTagsNotEvents, emitted: $emitted"
            )
            .isEqualTo(emitted)
    }

    @Test
    fun `every record event the platform publishes is either declared or excepted`() {
        val published = eventTypeConstantsInPackage("ru.citeck.ecos.events2.type")
            .filter { it.startsWith("record-") }
            .toSet()
        assertThat(published)
            .describedAs("record event types must be readable from ru.citeck.ecos.events2.type")
            .isNotEmpty

        val declared = declaredEventNames().filter { it.startsWith("record-") }.toSet()

        assertThat(declared + recordEventsDeliberatelyNotSubscribable)
            .describedAs(
                "EcosEventType must declare every record event on the bus, or except it explicitly. " +
                    "Declared: $declared, excepted: $recordEventsDeliberatelyNotSubscribable, " +
                    "published: $published"
            )
            .isEqualTo(published)
    }

    @Test
    fun `no event name is declared twice`() {
        val declared = EcosEventType.entries.flatMap { it.availableEventNames() }

        assertThat(declared).doesNotHaveDuplicates()
    }

    private fun declaredEventNames(): Set<String> {
        return EcosEventType.entries.flatMap { it.availableEventNames() }.toSet()
    }

    private fun constantsOf(className: String, namePrefix: String): Set<String> {
        val clazz = Class.forName(className)
        return clazz.declaredFields
            .filter {
                Modifier.isStatic(it.modifiers) &&
                    it.type == String::class.java &&
                    it.name.startsWith(namePrefix)
            }
            .mapNotNullTo(HashSet()) {
                it.isAccessible = true
                it.get(null) as? String
            }
    }

    /**
     * Every `companion object { const val TYPE }` in the package — a Kotlin `const val` in a companion
     * compiles to a static field on the OUTER class, so the event classes are found by looking for a
     * static String field named `TYPE`.
     */
    private fun eventTypeConstantsInPackage(basePackage: String): Set<String> {
        val resolver = PathMatchingResourcePatternResolver()
        val metadataReaderFactory = CachingMetadataReaderFactory(resolver)
        val resources = resolver.getResources(
            "classpath*:" + basePackage.replace('.', '/') + "/**/*.class"
        )

        val result = HashSet<String>()
        for (resource in resources) {
            val className = metadataReaderFactory.getMetadataReader(resource).classMetadata.className
            val clazz = runCatching { Class.forName(className, false, javaClass.classLoader) }.getOrNull()
                ?: continue
            val field = runCatching { clazz.getDeclaredField("TYPE") }.getOrNull() ?: continue
            if (!Modifier.isStatic(field.modifiers) || field.type != String::class.java) {
                continue
            }
            field.isAccessible = true
            (field.get(null) as? String)?.let { result.add(it) }
        }
        return result
    }
}
