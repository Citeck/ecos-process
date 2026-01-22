package ru.citeck.ecos.process.domain.bpmn.kpi

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.process.domain.bpmn.elements.config.BPMN_PROCESS_ELEMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.kpi.processors.BpmnElementEvent
import ru.citeck.ecos.process.domain.bpmn.kpi.processors.BpmnKpiProcessor
import ru.citeck.ecos.rabbitmq.RabbitMqChannel
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.Predicates.empty
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.time.Instant

const val BPMN_ELEMENTS_KPI_QUEUE_NAME = "bpmn-elements-kpi-queue"

// retry ~5 min
const val BPMN_ELEMENTS_KPI_QUEUE_DELAY_MS = 500L

data class BpmnElementsKpiProcessingRequest(
    val event: DataValue,
    val eventType: String
)

private fun DataValue.toBpmnElementEventData(): BpmnElementEventData {
    return Json.mapper.convert(this, BpmnElementEventData::class.java)
        ?: error("Cannot convert event $this to BpmnElementEventData")
}

private data class BpmnElementEventData(
    @AttName("record.id")
    var id: String = "",

    @AttName("record.procInstanceId")
    var procInstanceId: String = "",

    @AttName("record.processRef?id")
    var processRef: EntityRef? = EntityRef.EMPTY,

    @AttName("record.procDefRef?id")
    var procDefRef: EntityRef? = EntityRef.EMPTY,

    @AttName("record.elementDefId")
    var elementDefId: String = "",

    @AttName("record.created")
    var created: Instant = Instant.MIN,

    @AttName("record.completed")
    var completed: Instant? = null,

    @AttName("record.document")
    var document: EntityRef? = EntityRef.EMPTY,

    @AttName("record.document._type?id")
    var documentType: EntityRef? = EntityRef.EMPTY
) {

    fun toBpmnElementEvent() = BpmnElementEvent(
        procInstanceRef = procInstanceId.toEntityRef(),
        processRef = processRef ?: EntityRef.EMPTY,
        procDefRef = procDefRef ?: EntityRef.EMPTY,
        activityId = elementDefId,
        created = created,
        completed = completed,
        document = document ?: EntityRef.EMPTY,
        documentType = documentType ?: EntityRef.EMPTY
    )
}

@Component
class BpmnElementsKpiListener(
    eventsService: EventsService,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private lateinit var outcomeChannel: RabbitMqChannel

    init {
        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            outcomeChannel = channel
            channel.declareQueuesWithRetrying(BPMN_ELEMENTS_KPI_QUEUE_NAME, BPMN_ELEMENTS_KPI_QUEUE_DELAY_MS)
        }

        eventsService.addListener<BpmnElementEventData> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(BpmnElementEventData::class.java)
            withFilter(
                eq("typeDef.id", BPMN_PROCESS_ELEMENT_TYPE)
            )
            withAction {
                log.trace { "Received crete event: \n${Json.mapper.toPrettyString(it)}" }
                sendToQueue(BpmnElementsKpiProcessingRequest(DataValue.of(it), RecordCreatedEvent.TYPE))
            }
        }

        eventsService.addListener<BpmnElementEventData> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(BpmnElementEventData::class.java)
            withFilter(
                Predicates.and(
                    eq("typeDef.id", BPMN_PROCESS_ELEMENT_TYPE),
                    eq("diff._has.completed?bool!", true),
                    empty("before.completed")
                )

            )
            withAction {
                log.trace { "Received changed event: \n${Json.mapper.toPrettyString(it)}" }
                sendToQueue(BpmnElementsKpiProcessingRequest(DataValue.of(it), RecordChangedEvent.TYPE))
            }
        }
    }

    private fun sendToQueue(event: BpmnElementsKpiProcessingRequest) {
        outcomeChannel.publishMsg(BPMN_ELEMENTS_KPI_QUEUE_NAME, event)
    }
}

@Component
@DependsOn("bpmnElementsKpiListener")
class BpmnElementsKpiMutationAsyncProcessor(
    private val bpmnKpiProcessors: List<BpmnKpiProcessor>,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection,

    @Value("\${ecos-process.bpmn.kpi.mutation-processor.consumer.count}")
    private val consumersCount: Int,

    @Value("\${ecos-process.bpmn.kpi.mutation-processor.consumer.prefetch}")
    private val prefetch: Int,

    @Value("\${ecos-process.bpmn.kpi.mutation-processor.consumer.retry.max-attempts}")
    private val maxAttempts: Int,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        log.info { "Starting BPMN elements kpi mutation async processor with $consumersCount consumers" }

        for (i in 1..consumersCount) {
            bpmnRabbitmqConnection.doWithNewChannel(prefetch, false) { channel ->
                channel.addConsumerWithRetrying(
                    BPMN_ELEMENTS_KPI_QUEUE_NAME,
                    BpmnElementsKpiProcessingRequest::class.java,
                    maxAttempts
                ) { request, _ ->
                    AuthContext.runAsSystem {
                        TxnContext.doInNewTxn {
                            val msg = request.getContent()
                            log.trace { "Received request: \n${Json.mapper.toPrettyString(msg)}" }
                            when (msg.eventType) {
                                RecordCreatedEvent.TYPE -> handleCreateEvent(msg.event.toBpmnElementEventData())
                                RecordChangedEvent.TYPE -> handleChangedEvent(msg.event.toBpmnElementEventData())
                                else -> error("Unknown event type: ${msg.eventType}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleCreateEvent(event: BpmnElementEventData) {
        if (event.procInstanceId.isBlank() || (event.processRef?.isEmpty() == true) || event.elementDefId.isBlank()) {
            error(
                "Cannot handle create event with empty procInstanceId, processId or elementDefId: " +
                    "${Json.mapper.toPrettyString(event)}"
            )
        }

        val bpmnElementEvent = event.toBpmnElementEvent()
        if (bpmnElementEvent.completed == null) {
            bpmnKpiProcessors.forEach { it.processStartEvent(bpmnElementEvent) }
        } else {
            bpmnKpiProcessors.forEach { it.processStartEvent(bpmnElementEvent) }
            bpmnKpiProcessors.forEach { it.processEndEvent(bpmnElementEvent) }
        }
    }

    private fun handleChangedEvent(event: BpmnElementEventData) {
        if (event.procInstanceId.isBlank() ||
            (event.processRef?.isEmpty() == true) ||
            event.elementDefId.isBlank() ||
            event.completed == null
        ) {
            error(
                "Cannot handle changed event with empty procInstanceId, processId or elementDefId: " +
                    "${Json.mapper.toPrettyString(event)}"
            )
        }

        val bpmnElementEvent = event.toBpmnElementEvent()
        bpmnKpiProcessors.forEach { it.processEndEvent(bpmnElementEvent) }
    }
}
