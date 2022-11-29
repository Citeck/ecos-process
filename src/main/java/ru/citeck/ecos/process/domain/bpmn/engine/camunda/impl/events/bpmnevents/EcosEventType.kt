package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

enum class EcosEventType(
    val eventRepresentations: List<EventRepresentation>
) {
    UNDEFINED(emptyList()),

    COMMENT_CREATE(
        listOf(
            EventRepresentation(
                eventName = "comment-create",
                defaultModel = mapOf("text" to "text", "commentRecord" to "commentRecord?id")
            ),
            EventRepresentation(
                eventName = "ecos.comment.create",
                defaultModel = mapOf("text" to "textAfter", "commentRecord" to "commentRec?id")
            )
        )
    ),
    COMMENT_UPDATE(
        listOf(
            EventRepresentation(
                eventName = "comment-update",
                defaultModel = mapOf(
                    "textBefore" to "textBefore",
                    "textAfter" to "textAfter",
                    "commentRecord" to "commentRecord?id"
                )
            ),
            EventRepresentation(
                eventName = "ecos.comment.update",
                defaultModel = mapOf(
                    "textBefore" to "textBefore",
                    "textAfter" to "textAfter",
                    "commentRecord" to "commentRec?id"
                )
            )
        )
    ),
    COMMENT_DELETE(
        listOf(
            EventRepresentation(
                eventName = "comment-delete",
                defaultModel = mapOf("text" to "text", "commentRecord" to "commentRecord?id")
            ),
            EventRepresentation(
                eventName = "ecos.comment.delete",
                defaultModel = mapOf("text" to "textBefore", "commentRecord" to "commentRec?id")
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
    val defaultModel: Map<String, String>
)
