package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.digest.DigestUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.io.Serializable

private const val COMPOSED_EVENT_NAME_MAX_LENGTH = 255
private const val DELIMITER = ";"
private const val PREDICATE_CHECKSUM_PREFIX = "pr_"

/**
 * Composed event name is a string that contains [event], [record] and [type] separated by [DELIMITER]
 * with ending [DELIMITER].
 * Example: COMMENT_CREATE;ANY;emodel/type@doc;
 *
 * @author Roman Makarskiy
 */
data class ComposedEventName(
    val event: String,
    val record: String = RECORD_ANY,
    val type: String = TYPE_ANY
) : Serializable {

    init {
        require(event.isNotEmpty()) { "Event cannot be empty: $this" }
        require(record.isNotEmpty()) { "Record cannot be empty: $this" }
        require(type.isNotEmpty()) { "Type cannot be empty: $this" }

        require(event.contains(DELIMITER).not()) { "Event cannot contain delimiter: $this" }
        require(record.contains(DELIMITER).not()) { "Record cannot contain delimiter: $this" }
        require(type.contains(DELIMITER).not()) { "Type cannot contain delimiter: $this" }

        val composedString = toComposedString()
        require((composedString.length > COMPOSED_EVENT_NAME_MAX_LENGTH).not()) {
            "Composed event name cannot be longer than $COMPOSED_EVENT_NAME_MAX_LENGTH characters. " +
                "Current length: ${composedString.length} of $composedString"
        }
    }

    companion object {
        const val RECORD_ANY = "ANY"
        const val TYPE_ANY = "ANY"

        private const val serialVersionUID = 1L

        fun fromString(composedEventName: String): ComposedEventName {
            val parts = composedEventName.split(DELIMITER)
            return when (parts.size) {
                4 -> {
                    val predicateChecksum = parts[3]
                    if (predicateChecksum.isNotBlank() && !predicateChecksum.startsWith(PREDICATE_CHECKSUM_PREFIX)) {
                        throw IllegalArgumentException("Invalid predicate checksum format: $predicateChecksum")
                    }
                    ComposedEventName(parts[0], parts[1], parts[2])
                }

                else -> throw IllegalArgumentException(
                    "Composed event name must contain 4 parts. Current: $composedEventName"
                )
            }
        }
    }

    fun toComposedString(): String {
        val parts: MutableList<String> = mutableListOf(event, record, type)
        return parts.joinToString(DELIMITER) + DELIMITER
    }

    fun toComposedStringWithPredicateChecksum(predicate: Predicate): String {
        val checksum = DigestUtils.getMD5(Json.mapper.toBytes(predicate) ?: ByteArray(0)).hash

        return toComposedString() + PREDICATE_CHECKSUM_PREFIX + checksum
    }
}
