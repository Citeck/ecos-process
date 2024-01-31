package ru.citeck.ecos.process.domain.bpmn.elements

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery

@Component
class BpmnElementsEventListener(
    eventsService: EventsService,
    private val bpmnElementMutationAsyncProvider: BpmnElementMutationAsyncProvider
) {

    init {
        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_CREATE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.createTaskElement(event, false)
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_COMPLETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.completeTask(event)
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_DELETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.reactOnDeleteTaskEvent(event)
            }
        }

        eventsService.addListener<FlowElementEvent> {
            withEventType(BPMN_EVENT_ACTIVITY_ELEMENT_START)
            withDataClass(FlowElementEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.createStartActivityFlowElement(event)
            }
        }

        eventsService.addListener<FlowElementEvent> {
            withEventType(BPMN_EVENT_ACTIVITY_ELEMENT_END)
            withDataClass(FlowElementEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.creteEndActivityFlowElement(event)
            }
        }

        eventsService.addListener<FlowElementEvent> {
            withEventType(BPMN_EVENT_FLOW_ELEMENT_TAKE)
            withDataClass(FlowElementEvent::class.java)
            withAction { event ->
                bpmnElementMutationAsyncProvider.createTakeFlowElement(event)
            }
        }
    }

}


@Configuration
class BpmnElementMutateTaskExecutorConfig {

    @Bean(name = ["bpmnElAsync"])
    fun taskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        // Only one thread because have error with records unique constraint "ecos_record_ref___ext_id_idx"
        executor.corePoolSize = 1
        executor.initialize()
        return executor
    }
}

@Component
class BpmnElementMutationAsyncProvider(
    private val recordsService: RecordsService,
    @Qualifier("bpmnElAsync") private val bpmnElementAsyncExecutor: ThreadPoolTaskExecutor
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private fun execute(action: () -> Unit) {
        bpmnElementAsyncExecutor.execute {
            AuthContext.runAsSystem {
                action()
            }
        }
    }

    fun completeTask(event: UserTaskEvent) {
        execute {
            val existingElement = recordsService.queryOne(
                RecordsQuery.create {
                    withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("executionId", event.executionId),
                            Predicates.eq("elementId", event.taskId?.toString())
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
    }

    fun reactOnDeleteTaskEvent(event: UserTaskEvent) {
        execute {
            val existingElement = recordsService.queryOne(
                RecordsQuery.create {
                    withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("executionId", event.executionId),
                            Predicates.eq("elementId", event.taskId?.toString())
                        )
                    )
                }
            )
            if (existingElement == null) {
                log.warn {
                    "Cannot process user task delete event, because element not found: \n${event.toPrettyString()}"
                }
                return@execute
            }

            val data = ObjectData.create()
            data["completed"] = event.time
            data["comment"] = event.comment
            recordsService.mutate(existingElement, data)
        }
    }

    fun createTaskElement(event: UserTaskEvent, completedEvent: Boolean) {
        execute {
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
    }

    fun createStartActivityFlowElement(event: FlowElementEvent) {
        execute {
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
    }

    fun creteEndActivityFlowElement(event: FlowElementEvent) {
        execute {
            check(event.elementType != "UserTask") {
                "User task event not allowed for end flow element: $event"
            }
            log.trace { "Create end BPMN element by event: $event" }

            val existingElement = recordsService.queryOne(
                RecordsQuery.create {
                    withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("elementDefId", event.elementDefId),
                            Predicates.eq("executionId", event.executionId),
                        )
                    )
                }
            )
            if (existingElement == null) {
                log.warn { "End BPMN element not found for event: $event" }
                return@execute
            }

            val data = ObjectData.create()
            data["completed"] = event.time

            recordsService.mutate(existingElement, data)
        }
    }

    fun createTakeFlowElement(event: FlowElementEvent) {
        execute {
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
}
