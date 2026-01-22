package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.events2.type.RecordChangedEvent

enum class EcosEventType(
    val eventRepresentations: List<EventRepresentation>
) {
    UNDEFINED(emptyList()),

    COMMENT_CREATE(
        listOf(
            EventRepresentation(
                eventName = "comment-create",
                defaultModel = mapOf(
                    "text" to "text",
                    "commentRecord" to "commentRecord?id",
                    "attachments" to "attachments[]?id"
                )
            ),
            EventRepresentation(
                eventName = "ecos.comment.create",
                defaultModel = mapOf(
                    "text" to "textAfter",
                    "commentRecord" to "commentRec?id"
                )
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
    ),
    RECORD_STATUS_CHANGED(
        listOf(
            EventRepresentation(
                eventName = "record-status-changed",
                defaultModel = mapOf(
                    "before" to "before",
                    "after" to "after"
                )
            )
        )
    ),
    RECORD_CHANGED(
        listOf(
            EventRepresentation(
                eventName = RecordChangedEvent.TYPE,
                defaultModel = mapOf(
                    "before" to "before?json",
                    "after" to "after?json"
                )
            )
        )
    ),
    RECORD_CREATED(
        listOf(
            EventRepresentation(
                eventName = "record-created",
                defaultModel = emptyMap()
            )
        )
    ),
    RECORD_DELETED(
        listOf(
            EventRepresentation(
                eventName = "record-deleted",
                defaultModel = emptyMap()
            )
        )
    ),
    USER_EVENT(emptyList());

    companion object {

        const val RECORD_ATT = "record"
        const val RECORD_TYPE_ATT = "recordType"

        fun from(value: String): EcosEventType = EcosEventType.entries.find { event ->
            event.eventRepresentations.any { it.eventName == value }
        } ?: let {
            EcosEventType.entries.find { it.name == value } ?: UNDEFINED
        }

        fun findRepresentation(eventName: String): EventRepresentation? {
            return from(eventName).representation(eventName)
        }
    }

    private val representation = fun(eventName: String): EventRepresentation? {
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
