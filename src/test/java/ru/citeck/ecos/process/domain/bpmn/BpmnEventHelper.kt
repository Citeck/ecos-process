package ru.citeck.ecos.process.domain.bpmn

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordDeletedEvent
import ru.citeck.ecos.events2.type.RecordRefChangedEvent
import ru.citeck.ecos.model.lib.type.dto.TypeInfo
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef

const val TEST_USER = "testUser"

@Component
class BpmnEventHelper(
    private val eventsService: EventsService,

    @Value("\${spring.application.name}")
    private val appName: String
) {

    private val commentCreateEmitter = eventsService.getEmitter(
        EmitterConfig.create<CommentCreateEvent> {
            source = appName
            eventType = CommentCreateEvent.TYPE
            eventClass = CommentCreateEvent::class.java
        }
    )

    private val commentUpdateEmitter = eventsService.getEmitter(
        EmitterConfig.create<CommentUpdateEvent> {
            source = appName
            eventType = CommentUpdateEvent.TYPE
            eventClass = CommentUpdateEvent::class.java
        }
    )

    private val recordChangedEmitter = eventsService.getEmitter(
        EmitterConfig.create<RecordChangedEventDto> {
            source = appName
            eventType = RecordChangedEvent.TYPE
            eventClass = RecordChangedEventDto::class.java
        }
    )

    private val recordDeletedEmitter = eventsService.getEmitter(
        EmitterConfig.create<RecordDeletedEventDto> {
            source = appName
            eventType = RecordDeletedEvent.TYPE
            eventClass = RecordDeletedEventDto::class.java
        }
    )

    private val recordRefChangedEmitter = eventsService.getEmitter(
        EmitterConfig.create<RecordRefChangedEventDto> {
            source = appName
            eventType = RecordRefChangedEvent.TYPE
            eventClass = RecordRefChangedEventDto::class.java
        }
    )

    fun sendManualEvent(eventName: String, eventData: Map<String, Any>) {
        AuthContext.runAs(TEST_USER) {
            eventsService.getEmitter(
                EmitterConfig.create<Any> {
                    source = appName
                    eventType = eventName
                    eventClass = Any::class.java
                }
            ).emit(eventData)
        }
    }

    fun sendCreateCommentEvent(event: CommentCreateEvent) {
        AuthContext.runAs(TEST_USER) {
            commentCreateEmitter.emit(event)
        }
    }

    fun sendCreateCommentEvent(event: CommentCreateEvent, asUser: String) {
        AuthContext.runAs(asUser) {
            commentCreateEmitter.emit(event)
        }
    }

    fun sendUpdateCommentEvent(event: CommentUpdateEvent) {
        commentUpdateEmitter.emit(event)
    }

    fun sendRecordChangedEvent(event: RecordChangedEventDto) {
        recordChangedEmitter.emit(event)
    }

    fun sendRecordDeletedEvent(event: RecordDeletedEventDto) {
        recordDeletedEmitter.emit(event)
    }

    fun sendRecordRefChangedEvent(event: RecordRefChangedEventDto) {
        recordRefChangedEmitter.emit(event)
    }
}

data class CommentCreateEvent(
    val record: EntityRef,
    val commentRecord: EntityRef,
    val text: String? = null
) {

    companion object {
        const val TYPE = "comment-create"
    }
}

data class CommentUpdateEvent(
    val record: EntityRef,
    val commentRecord: EntityRef,
    val textBefore: String? = null,
    val textAfter: String? = null
) {

    companion object {
        const val TYPE = "comment-update"
    }
}

data class RecordChangedEventDto(
    val record: EntityRef,
    val diff: Diff,
    val after: Map<String, Any?> = emptyMap(),
    val typeDef: TypeInfo? = null
)

class Diff(
    val list: List<ChangedValue>
) : AttValue {
    override fun has(name: String): Boolean {
        return list.any { it.id == name }
    }

    override fun getAtt(name: String): Any? {
        if (name == "list") {
            return list.map {
                RecordChangedEvent.DiffValue(it.id)
            }
        }

        return super.getAtt(name)
    }
}

data class ChangedValue(
    val id: String
)

data class RecordDeletedEventDto(
    val record: EntityRef
)

data class RecordRefChangedEventDto(
    val record: EntityRef,
    val before: EntityRef,
    val after: EntityRef
)
