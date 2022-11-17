package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_EVENT
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.EventType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class EcosEventToBpmnSignalListener(
    eventsService: EventsService,
    private val cRuntimeService: RuntimeService,
    private val commandExecutor: CommandExecutor
) {

    init {
        eventsService.addListener<CommentEvent> {
            withEventType(EventType.COMMENT_CREATE.value)
            withDataClass(CommentEvent::class.java)
            withAction { event -> fireSignals(EventType.COMMENT_CREATE.value, event, event.toData()) }
        }

        eventsService.addListener<CommentEvent> {
            withEventType(EventType.COMMENT_UPDATE.value)
            withDataClass(CommentEvent::class.java)
            withAction { event -> fireSignals(EventType.COMMENT_UPDATE.value, event, event.toData()) }
        }

        eventsService.addListener<CommentEvent> {
            withEventType(EventType.COMMENT_DELETE.value)
            withDataClass(CommentEvent::class.java)
            withAction { event -> fireSignals(EventType.COMMENT_DELETE.value, event, event.toData()) }
        }
    }

    private fun fireSignals(eventType: String, eventData: CommentEvent, recordData: RecordData) {
        /*sendSignal(eventData, "${eventType}\$${FilterEventByRecord.ANY.name}")

        val (record, type) = recordData

        record?.let {
            sendSignal(eventData, "${eventType}\$$record")
        }

        type?.let {
            sendSignal(eventData, "${eventType}\$${FilterEventByRecord.ANY.name}\$$type")
        }*/

       /* cRuntimeService.createMessageCorrelation("message1")
            .correlateAll()*/

        val idCmd = SignalByIdCmd()
        commandExecutor.execute(idCmd)
    }

    private fun sendSignal(eventData: CommentEvent, signalName: String) {

        cRuntimeService.createSignalEvent(signalName)
            .setVariables(
                mapOf(
                    VAR_EVENT to mapOf(
                        "rec" to eventData.rec.toString(),
                        "commentRec" to eventData.commentRec.toString(),
                        "textBefore" to eventData.textBefore,
                        "textAfter" to eventData.textAfter
                    )
                )
            )
            .send()
    }
}

private data class RecordData(
    val record: EntityRef? = null,
    val type: EntityRef? = null
)

data class CommentEvent(
    val rec: RecordRef? = null,
    val commentRec: RecordRef? = null,
    val textBefore: String? = "",
    val textAfter: String? = "",

    @AttName("rec._type?id")
    val recType: RecordRef? = null,
)

private fun CommentEvent.toData(): RecordData {
    return RecordData(
        record = rec,
        type = recType
    )
}
