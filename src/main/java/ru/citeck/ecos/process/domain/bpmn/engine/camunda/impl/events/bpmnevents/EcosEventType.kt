package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordContentChangedEvent
import ru.citeck.ecos.events2.type.RecordDraftStatusChangedEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_PROCESS_START
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_ASSIGN
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_COMPLETE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_COMPLETE_ERROR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_CREATE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_DELETE

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

    /**
     * A record entered or left the draft state.
     *
     * `before` / `after` are booleans and MUST be read with `?bool`: a bare read resolves through `?disp`
     * to the strings `"true"` / `"false"`, which a boolean predicate never matches. `after == false` means
     * the record was submitted. Fires on the flip only — a record created already non-draft produces
     * [RECORD_CREATED] instead (see `BpmnProcessAutoStarter`, which listens to both).
     */
    RECORD_DRAFT_STATUS_CHANGED(
        listOf(
            EventRepresentation(
                eventName = RecordDraftStatusChangedEvent.TYPE,
                defaultModel = mapOf(
                    "before" to "before?bool",
                    "after" to "after?bool"
                )
            )
        )
    ),

    /**
     * The default content of an EXISTING record was added, replaced or removed.
     *
     * No default model: `before` / `after` are the content values themselves, so the useful reading
     * (file name, size, mimetype, body) depends on the subscriber and is stated in its own model.
     *
     * Not emitted when the content arrives in the same mutation that creates the record
     * (`DbRecEventsHandler` returns early for a new record) — an upload through the documents widget
     * needs [RECORD_CREATED] as well. A file in that widget is its own child record, so the event's
     * `record` is the FILE, not the case: a subscription narrowed to the current document will not see it.
     */
    RECORD_CONTENT_CHANGED(
        listOf(
            EventRepresentation(
                eventName = RecordContentChangedEvent.TYPE,
                defaultModel = emptyMap()
            )
        )
    ),

    /**
     * A user task got an assignee — claimed from a pool, or assigned by someone else. Emitted by
     * `BpmnTaskAssignEventListener` on the camunda `assignment` event, so it also fires for a task
     * auto-assigned to its single recipient at creation.
     *
     * The model requests `assigneeRef`; the payload also carries the bare `assignee` login for a
     * subscription that asks for it.
     */
    USER_TASK_ASSIGN(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_USER_TASK_ASSIGN,
                defaultModel = mapOf(
                    "taskId" to "taskId?id",
                    "assigneeRef" to "assigneeRef?id",
                    "elementDefId" to "elementDefId"
                )
            )
        )
    ),

    /**
     * A user task was created.
     *
     * Carries the recipient shape, which [USER_TASK_ASSIGN] cannot: a task offered to a pool has no
     * assignee and a populated candidate list. Only the refs are requested; the bare authority names are
     * in the payload.
     *
     * A user task in multi-instance AUTO mode gets `MultiInstanceAutoModeUserTaskAssignListener` instead
     * and emits nothing here.
     */
    USER_TASK_CREATE(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_USER_TASK_CREATE,
                defaultModel = mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "assigneeRef" to "assigneeRef?id",
                    "candidateUsersRef" to "candidateUsersRef[]?id",
                    "candidateGroupsRef" to "candidateGroupsRef[]?id"
                )
            )
        )
    ),

    /**
     * A user task was completed.
     *
     * `completedBy` is the actor and is not the same as the assignee — a task completed on behalf of
     * someone else differs in the two. `outcome` is the stable outcome id; the localized label is not
     * requested (route on the id).
     */
    USER_TASK_COMPLETE(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_USER_TASK_COMPLETE,
                defaultModel = mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "outcome" to "outcome",
                    "completedBy" to "completedBy",
                    "comment" to "comment",
                    "assigneeRef" to "assigneeRef?id"
                )
            )
        )
    ),

    /**
     * A user task completion FAILED and the transaction was rolled back — typically a completion guard.
     *
     * `errorMessage` is requested; `errorStackTrace` is in the payload but not in the model.
     */
    USER_TASK_COMPLETE_ERROR(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_USER_TASK_COMPLETE_ERROR,
                defaultModel = mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "outcome" to "outcome",
                    "completedBy" to "completedBy",
                    "errorMessage" to "errorMessage"
                )
            )
        )
    ),

    /**
     * A user task was deleted — cancelled by a boundary event, or removed with its process instance.
     *
     * Not emitted for a normal completion: `TaskEntity.delete` reaches the deleted state only when the
     * delete reason is not `completed`.
     */
    USER_TASK_DELETE(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_USER_TASK_DELETE,
                defaultModel = mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "assigneeRef" to "assigneeRef?id"
                )
            )
        )
    ),

    /**
     * A process instance was started.
     *
     * Narrowing by document works through `ProcessStartEvent.record`, which is an alias of `document`.
     */
    PROCESS_START(
        listOf(
            EventRepresentation(
                eventName = BPMN_EVENT_PROCESS_START,
                defaultModel = mapOf(
                    "processKey" to "processKey",
                    "processInstanceId" to "processInstanceId",
                    "document" to "document?id"
                )
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
