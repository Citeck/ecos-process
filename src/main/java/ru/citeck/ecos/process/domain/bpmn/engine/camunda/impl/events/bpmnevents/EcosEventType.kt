package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

enum class EcosEventType(
    val eventsRepresentation: List<EventRepresentation>
) {
    UNDEFINED(emptyList()),

    COMMENT_CREATE(
        listOf(
            EventRepresentation(
                eventName = "comment-create",
                recordAttribute = "record",
                defaultAttributes = mapOf("text" to "text", "commentRecord" to "commentRecord")
            ),
            EventRepresentation(
                eventName = "ecos.comment.create",
                recordAttribute = "rec",
                defaultAttributes = mapOf("text" to "textAfter", "commentRecord" to "commentRec")
            )
        )
    ),
    COMMENT_UPDATE(
        listOf(
            EventRepresentation(
                eventName = "comment-update",
                recordAttribute = "record",
                defaultAttributes = mapOf(
                    "textBefore" to "textBefore",
                    "textAfter" to "textAfter",
                    "commentRecord" to "commentRecord"
                )
            ),
            EventRepresentation(
                eventName = "ecos.comment.update",
                recordAttribute = "rec",
                defaultAttributes = mapOf(
                    "textBefore" to "textBefore",
                    "textAfter" to "textAfter",
                    "commentRecord" to "commentRec"
                )
            )
        )
    ),
    COMMENT_DELETE(
        listOf(
            EventRepresentation(
                eventName = "comment-delete",
                recordAttribute = "record",
                defaultAttributes = mapOf("text" to "text", "commentRecord" to "commentRecord")
            ),
            EventRepresentation(
                eventName = "ecos.comment.delete",
                recordAttribute = "rec",
                defaultAttributes = mapOf("text" to "textBefore", "commentRecord" to "commentRec")
            )
        )
    );

    val representation = fun(eventName: String): EventRepresentation? {
        return eventsRepresentation.firstOrNull { it.eventName == eventName }
    }

    val availableEventNames = fun(): List<String> {
        return eventsRepresentation.map { it.eventName }
    }

    companion object {
        fun from(value: String): EcosEventType =
            EcosEventType.values().find { event ->
                event.eventsRepresentation.any { it.eventName == value }
            } ?: let {
                EcosEventType.values().find { it.name == value } ?: UNDEFINED
            }

        fun findRepresentation(eventName: String): EventRepresentation? {
            return from(eventName).representation(eventName)
        }
    }
}

data class EventRepresentation(
    val eventName: String,
    val recordAttribute: String,
    val defaultAttributes: Map<String, String>
)
