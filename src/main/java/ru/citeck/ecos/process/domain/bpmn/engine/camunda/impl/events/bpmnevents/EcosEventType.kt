package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

enum class EcosEventType(
    val eventRepresentations: List<EventRepresentation>
) {
    UNDEFINED(emptyList()),

    COMMENT_CREATE(
        listOf(
            EventRepresentation(
                eventName = "comment-create",
                recordAttribute = "record",
                defaultModel = mapOf("text" to "text", "commentRecord" to "commentRecord")
            ),
            EventRepresentation(
                eventName = "ecos.comment.create",
                recordAttribute = "rec",
                defaultModel = mapOf("text" to "textAfter", "commentRecord" to "commentRec")
            )
        )
    ),
    COMMENT_UPDATE(
        listOf(
            EventRepresentation(
                eventName = "comment-update",
                recordAttribute = "record",
                defaultModel = mapOf(
                    "textBefore" to "textBefore",
                    "textAfter" to "textAfter",
                    "commentRecord" to "commentRecord"
                )
            ),
            EventRepresentation(
                eventName = "ecos.comment.update",
                recordAttribute = "rec",
                defaultModel = mapOf(
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
                defaultModel = mapOf("text" to "text", "commentRecord" to "commentRecord")
            ),
            EventRepresentation(
                eventName = "ecos.comment.delete",
                recordAttribute = "rec",
                defaultModel = mapOf("text" to "textBefore", "commentRecord" to "commentRec")
            )
        )
    );

    companion object {
        fun from(value: String): EcosEventType =
            EcosEventType.values().find { event ->
                event.eventRepresentations.any { it.eventName == value }
            } ?: let {
                EcosEventType.values().find { it.name == value } ?: UNDEFINED
            }

        fun findRepresentation(eventName: String): EventRepresentation? {
            return from(eventName).representation(eventName)
        }
    }

    val representation = fun(eventName: String): EventRepresentation? {
        return eventRepresentations.firstOrNull { it.eventName == eventName }
    }

    val availableEventNames = fun(): List<String> {
        return eventRepresentations.map { it.eventName }
    }
}

data class EventRepresentation(
    val eventName: String,
    val recordAttribute: String,
    val defaultModel: Map<String, String>
)
