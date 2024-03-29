package ru.citeck.ecos.process.domain.bpmn.event

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import kotlin.test.assertEquals

private const val EVENT_TYPE = "comment.added"
private const val DOCUMENT_TYPE = "emodel/type@doc"
private const val DOCUMENT = "int/doc@1"

/**
 * @author Roman Makarskiy
 */
class ComposedEventNameTest {

    @Test
    fun `create composed event name from full string`() {
        val composedEventName = ComposedEventName.fromString(
            "$EVENT_TYPE;${ComposedEventName.RECORD_ANY};$DOCUMENT_TYPE;pr_0"
        )

        assertEquals(
            ComposedEventName(EVENT_TYPE, ComposedEventName.RECORD_ANY, DOCUMENT_TYPE),
            composedEventName
        )
    }

    @Test
    fun `create composed event name with document as expression`() {
        val document = "\${someVariable}"

        val composedEventName = ComposedEventName.fromString(
            "$EVENT_TYPE;$document;$DOCUMENT_TYPE;pr_0"
        )

        assertEquals(
            ComposedEventName(EVENT_TYPE, document, DOCUMENT_TYPE),
            composedEventName
        )
    }

    @Test
    fun `create composed event name from event and document`() {
        val composedEventName = ComposedEventName.fromString(
            "$EVENT_TYPE;$DOCUMENT;${ComposedEventName.TYPE_ANY};pr_0"
        )

        assertEquals(
            ComposedEventName(EVENT_TYPE, DOCUMENT),
            composedEventName
        )
    }

    @Test
    fun `create composed event name from event document and document type`() {
        val composedEventName = ComposedEventName.fromString(
            "$EVENT_TYPE;$DOCUMENT;$DOCUMENT_TYPE;pr_0"
        )

        assertEquals(
            ComposedEventName(EVENT_TYPE, DOCUMENT, DOCUMENT_TYPE),
            composedEventName
        )
    }

    @Test
    fun `composed event name to composed string`() {
        val composedEventName = ComposedEventName(EVENT_TYPE, DOCUMENT, DOCUMENT_TYPE)
        assertEquals(
            "$EVENT_TYPE;$DOCUMENT;$DOCUMENT_TYPE;",
            composedEventName.toComposedString()
        )
    }

    @Test
    fun `composed event name to composed string without document type`() {
        val composedEventName = ComposedEventName(EVENT_TYPE, DOCUMENT)
        assertEquals(
            "$EVENT_TYPE;$DOCUMENT;${ComposedEventName.TYPE_ANY};",
            composedEventName.toComposedString()
        )
    }

    @Test
    fun `composed event name with any type and any document to composed string`() {
        val composedEventName = ComposedEventName(EVENT_TYPE, ComposedEventName.RECORD_ANY, ComposedEventName.TYPE_ANY)
        assertEquals(
            "$EVENT_TYPE;${ComposedEventName.RECORD_ANY};${ComposedEventName.TYPE_ANY};",
            composedEventName.toComposedString()
        )
    }

    @Test
    fun `create composed event name from invalid string must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName.fromString("invalid string")
        }
    }

    @Test
    fun `create composed event name from empty string must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName.fromString("")
        }
    }

    @Test
    fun `create composed event name from string with empty event must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName(
                "",
                DOCUMENT
            )
        }
    }

    @Test
    fun `create composed event name from string with empty document must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName(
                EVENT_TYPE,
                ""
            )
        }
    }

    @Test
    fun `create composed event name from string with empty type must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName(EVENT_TYPE, DOCUMENT, "")
        }
    }

    @Test
    fun `create composed event name from string with redundant delimiter must throw`() {
        assertThrows<IllegalArgumentException> {
            ComposedEventName.fromString(
                "event;typ;e;doc"
            )
        }
    }

    @Test
    fun `composed event name must throw if composed string more then max length`() {
        val event200 = "a".repeat(200)
        val document200 = "b".repeat(200)

        assertThrows<IllegalArgumentException> {
            ComposedEventName(event200, document200)
        }
    }
}
