package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import java.io.Serializable

private const val COMPOSED_EVENT_NAME_MAX_LENGTH = 255
private const val DELIMITER = ";"

/**
 * Composed event name is a string that contains [event], [record] and [type] separated by [DELIMITER].
 * Example: COMMENT_CREATE;ANY;emodel/type@doc
 *
 * @author Roman Makarskiy
 */
data class ComposedEventName(
    val event: String,
    val record: String,
    val type: String? = null
) : Serializable {

    init {
        require(event.isNotEmpty()) { "Event cannot be empty: $this" }
        require(record.isNotEmpty()) { "Record cannot be empty: $this" }

        require(event.contains(DELIMITER).not()) { "Event cannot contain delimiter: $this" }
        require(record.contains(DELIMITER).not()) { "Record cannot contain delimiter: $this" }

        type?.contains(DELIMITER)?.let { require(it.not()) { "Type cannot contain delimiter: $this" } }

        val composedString = toComposedString()
        require((composedString.length > COMPOSED_EVENT_NAME_MAX_LENGTH).not()) {
            "Composed event name cannot be longer than $COMPOSED_EVENT_NAME_MAX_LENGTH characters. " +
                "Current length: ${composedString.length} of $composedString"
        }
    }

    companion object {
        const val RECORD_ANY = "ANY"

        private const val serialVersionUID = 1L

        fun fromString(composedEventName: String): ComposedEventName {
            val parts = composedEventName.split(DELIMITER)
            return when (parts.size) {
                2 -> ComposedEventName(parts[0], parts[1])
                3 -> ComposedEventName(parts[0], parts[1], parts[2])
                else -> throw IllegalArgumentException("Composed event name must contain 2 or 3 parts")
            }
        }
    }

    fun toComposedString(): String {
        val parts: MutableList<String> = mutableListOf(event, record)
        if (!type.isNullOrBlank()) {
            parts += type
        }

        return parts.joinToString(DELIMITER)
    }
}
