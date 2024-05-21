package ru.citeck.ecos.process.domain.bpmn.elements

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ELEMENT_DEF_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ELEMENT_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_PROCESS_INSTANCE_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FullFulFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.RawFlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.rabbitmq.RabbitMqChannel
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.txn.lib.TxnContext

const val BPMN_ELEMENT_PROCESSING_QUEUE_NAME = "bpmn-element-processing-flow"

// retry ~5 min
const val BPMN_ELEMENT_PROCESSING_QUEUE_RETRY_COUNT = 1200
const val BPMN_ELEMENT_PROCESSING_QUEUE_DELAY_MS = 500L

data class BpmnElementProcessingRequest(
    val event: DataValue,
    val eventType: String
)

private fun DataValue.toUserTaskEvent(): UserTaskEvent {
    return Json.mapper.convert(this, UserTaskEvent::class.java)
        ?: error("Cannot convert event $this to UserTaskEvent")
}

private fun DataValue.toRawFlowElementEvent(): RawFlowElementEvent {
    return Json.mapper.convert(this, RawFlowElementEvent::class.java)
        ?: error("Cannot convert event $this to FlowElementEvent")
}

@Component
@ConditionalOnProperty(name = ["ecos-process.bpmn.elements.listener.enabled"], havingValue = "true")
class BpmnElementsToQueuePublisher(
    eventsService: EventsService,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection
) {

    private lateinit var outcomeChannel: RabbitMqChannel

    init {
        bpmnRabbitmqConnection.doWithNewChannel { channel ->
            outcomeChannel = channel
            channel.declareQueuesWithRetrying(
                BPMN_ELEMENT_PROCESSING_QUEUE_NAME,
                BPMN_ELEMENT_PROCESSING_QUEUE_DELAY_MS
            )
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_CREATE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                sendToQueue(BpmnElementProcessingRequest(DataValue.of(event), BPMN_EVENT_USER_TASK_CREATE))
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_COMPLETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                sendToQueue(BpmnElementProcessingRequest(DataValue.of(event), BPMN_EVENT_USER_TASK_COMPLETE))
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_DELETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                sendToQueue(BpmnElementProcessingRequest(DataValue.of(event), BPMN_EVENT_USER_TASK_DELETE))
            }
        }
    }

    fun sendToQueue(request: BpmnElementProcessingRequest) {
        outcomeChannel.publishMsg(
            BPMN_ELEMENT_PROCESSING_QUEUE_NAME,
            request
        )
    }
}

@Component
@DependsOn("bpmnElementsToQueuePublisher")
@ConditionalOnProperty(name = ["ecos-process.bpmn.elements.listener.enabled"], havingValue = "true")
class BpmnElementMutationAsyncProcessor(
    private val recordsService: RecordsService,
    @Qualifier("bpmnRabbitmqConnection")
    bpmnRabbitmqConnection: RabbitMqConnection,

    @Value("\${ecos-process.bpmn.elements.mutation-processor.consumer.count}")
    private val consumersCount: Int,

    @Value("\${ecos-process.bpmn.elements.mutation-processor.consumer.prefetch}")
    private val prefetch: Int
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        log.info { "Starting BPMN element mutation async processor with $consumersCount consumers" }

        for (i in 1..consumersCount) {
            bpmnRabbitmqConnection.doWithNewChannel(prefetch, false) { channel ->
                channel.addConsumerWithRetrying(
                    BPMN_ELEMENT_PROCESSING_QUEUE_NAME,
                    BpmnElementProcessingRequest::class.java,
                    BPMN_ELEMENT_PROCESSING_QUEUE_RETRY_COUNT
                ) { request, _ ->
                    onMessageReceived(request.getContent(), "consumer-$i")
                }
            }
        }
    }

    private fun onMessageReceived(request: BpmnElementProcessingRequest, tag: String? = null) {
        log.debug {
            "Received BPMN element processing request with tag $tag, request: $request"
        }

        AuthContext.runAsSystem {
            TxnContext.doInNewTxn {
                when (request.eventType) {
                    BPMN_EVENT_USER_TASK_CREATE -> {
                        val event = request.event.toUserTaskEvent()
                        createTaskElement(event, false)
                    }

                    BPMN_EVENT_USER_TASK_COMPLETE -> {
                        val event = request.event.toUserTaskEvent()
                        completeTask(event)
                    }

                    BPMN_EVENT_USER_TASK_DELETE -> {
                        val event = request.event.toUserTaskEvent()
                        reactOnDeleteTaskEvent(event)
                    }

                    BPMN_EVENT_ACTIVITY_ELEMENT_START -> {
                        val event = request.event.toRawFlowElementEvent().toFullFullFlowElement()
                        createStartActivityFlowElement(event)
                    }

                    BPMN_EVENT_ACTIVITY_ELEMENT_END -> {
                        val event = request.event.toRawFlowElementEvent().toFullFullFlowElement()
                        creteEndActivityFlowElement(event)
                    }

                    BPMN_EVENT_FLOW_ELEMENT_TAKE -> {
                        val event = request.event.toRawFlowElementEvent().toFullFullFlowElement()
                        createTakeFlowElement(event)
                    }

                    else -> {
                        error("Unknown event type: ${request.eventType}")
                    }
                }
            }
        }
    }

    private fun completeTask(event: UserTaskEvent) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }

        log.trace { "Complete task element. Event: $event" }

        val existingElement = recordsService.queryOne(
            RecordsQuery.create {
                withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                withQuery(
                    Predicates.and(
                        Predicates.eq(BPMN_PROCESS_INSTANCE_ID, event.procInstanceId.toString()),
                        Predicates.eq(BPMN_ELEMENT_ID, event.taskId?.toString())
                    )
                )
            }
        )
        if (existingElement == null) {
            createTaskElement(event, true)
        } else {
            val data = ObjectData.create()
            data["completed"] = event.time
            data["outcome"] = event.outcome
            data["outcomeName"] = event.outcomeName
            data["comment"] = event.comment
            recordsService.mutate(existingElement, data)
        }
    }

    private fun reactOnDeleteTaskEvent(event: UserTaskEvent) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }

        log.trace { "React on delete task event: $event" }

        val existingElement = recordsService.queryOne(
            RecordsQuery.create {
                withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                withQuery(
                    Predicates.and(
                        Predicates.eq(BPMN_PROCESS_INSTANCE_ID, event.procInstanceId.toString()),
                        Predicates.eq(BPMN_ELEMENT_ID, event.taskId?.toString())
                    )
                )
            }
        )
        if (existingElement == null) {
            error("Cannot process user task delete event, because element not found: \n${event.toPrettyString()}")
        }

        val data = ObjectData.create()
        data["completed"] = event.time
        data["comment"] = event.comment
        recordsService.mutate(existingElement, data)
    }

    private fun createTaskElement(event: UserTaskEvent, completedEvent: Boolean) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }
        require(event.time != null) {
            "Time is empty for event: $event"
        }

        log.trace { "Create task element. Event: $event. Completed: $completedEvent" }

        val data = ObjectData.create(event)
        if (completedEvent) {
            data["started"] = event.time
            data["completed"] = event.time
        } else {
            data.remove("outcome")
            data.remove("outcomeName")
            data.remove("comment")
        }
        data["elementId"] = event.taskId?.toString()
        data["elementType"] = "UserTask"
        if (data["engine"].asText().isBlank()) {
            data["engine"] = "flowable"
        }
        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }

    private fun createStartActivityFlowElement(event: FullFulFlowElementEvent) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }
        require(event.time != null) {
            "Time is empty for event: $event"
        }
        check(event.elementType != "UserTask") {
            "User task event not allowed for start flow element: $event"
        }

        log.trace { "Create start BPMN element by event: $event" }

        val data = ObjectData.create(event)
        data["created"] = event.time
        if (data["engine"].asText().isBlank()) {
            data["engine"] = "flowable"
        }

        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }

    private fun creteEndActivityFlowElement(event: FullFulFlowElementEvent) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }
        require(event.time != null) {
            "Time is empty for event: $event"
        }
        check(event.elementType != "UserTask") {
            "User task event not allowed for end flow element: $event"
        }

        log.trace { "Create end BPMN element by event: $event" }

        val existingElement = recordsService.queryOne(
            RecordsQuery.create {
                withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                withQuery(
                    Predicates.and(
                        Predicates.eq(BPMN_ELEMENT_DEF_ID, event.elementDefId),
                        Predicates.eq(BPMN_PROCESS_INSTANCE_ID, event.procInstanceId.toString()),
                        // Its not ideal solution, but activity flow element does not have elementId in camunda engine
                        Predicates.empty("completed")
                    )
                )
            }
        ) ?: error("End BPMN element not found for event: $event")

        val data = ObjectData.create()
        data["completed"] = event.time

        recordsService.mutate(existingElement, data)
    }

    private fun createTakeFlowElement(event: FullFulFlowElementEvent) {
        require(event.procInstanceId?.isNotEmpty() == true) {
            "Process instance id is empty for event: $event"
        }
        check(event.elementType != "UserTask") {
            "User task event not allowed for take flow element: $event"
        }

        log.trace { "Create take flow BPMN element by event: $event" }

        val data = ObjectData.create(event)
        data["created"] = event.time
        data["completed"] = event.time
        if (data["engine"].asText().isBlank()) {
            data["engine"] = "flowable"
        }
        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }
}
