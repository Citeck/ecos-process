package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.COMPOSED_EVENT_NAME_DOCUMENT_ANY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventNameGenerator
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.IncomingEventData
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.EventType
import ru.citeck.ecos.webapp.api.entity.EntityRef

class ComposedEventNameGeneratorTest {

    @Test
    fun `generate composed event name from ecos event name with document and type`() {
        val ecosEventName = "comment.added"
        val document = EntityRef.valueOf("store/doc@1")
        val type = EntityRef.valueOf("emodel/type@doc")
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY),
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY, type.toString()),

            ComposedEventName(ecosEventName, document.toString()),
            ComposedEventName(ecosEventName, document.toString(), type.toString())
        )
    }

    @Test
    fun `generate composed event name from ecos event name with document`() {
        val ecosEventName = "comment.added"
        val document = EntityRef.valueOf("store/doc@1")
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY),
            ComposedEventName(ecosEventName, document.toString())
        )
    }

    @Test
    fun `generate composed event name from ecos event name with type`() {
        val ecosEventName = "comment.added"
        val document = EntityRef.EMPTY
        val type = EntityRef.valueOf("emodel/type@doc")
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY),
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY, type.toString())
        )
    }

    @Test
    fun `generate composed event name from ecos event name without document and type`() {
        val ecosEventName = "comment.added"
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY)
        )
    }

    @Test
    fun `generate composed event name from ecos event name with empty event name`() {
        val ecosEventName = ""
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).isEmpty()
    }

    @Test
    fun `generate composed event name from undefined event type should generate with source event name`() {
        val ecosEventName = "some-event"
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(ecosEventName, COMPOSED_EVENT_NAME_DOCUMENT_ANY)
        )
    }

    @Test
    fun `generate composed event name from existing event type name should generate enum event name`() {
        val ecosEventName = EventType.COMMENT_CREATE.name
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(EventType.COMMENT_CREATE.name, COMPOSED_EVENT_NAME_DOCUMENT_ANY)
        )
    }

    @Test
    fun `generate composed event name from existing event type should generate enum event name`() {
        val ecosEventName = EventType.COMMENT_CREATE.availableEventNames[0]
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(EventType.COMMENT_CREATE.name, COMPOSED_EVENT_NAME_DOCUMENT_ANY)
        )
    }

    @Test
    fun `generate composed event name from existing event type var2 should generate enum event name`() {
        val ecosEventName = EventType.COMMENT_CREATE.availableEventNames[1]
        val document = EntityRef.EMPTY
        val type = EntityRef.EMPTY
        val eventData = IncomingEventData(ecosEventName, document, type)

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(eventData)

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(EventType.COMMENT_CREATE.name, COMPOSED_EVENT_NAME_DOCUMENT_ANY)
        )
    }
}
