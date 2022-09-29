package ru.citeck.ecos.process.domain.bpmn.elements

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_FLOW_ELEMENT_START
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_COMPLETE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_CREATE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext

@Component
class BpmnElementsCreateListener(
    eventsService: EventsService,
    private val recordsService: RecordsService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_CREATE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                execPostTxnAction(event) {
                    createTaskElement(it, false)
                }
            }
        }

        eventsService.addListener<UserTaskEvent> {
            withEventType(BPMN_EVENT_USER_TASK_COMPLETE)
            withDataClass(UserTaskEvent::class.java)
            withAction { event ->
                execPostTxnAction(event) {
                    val existingElement = recordsService.queryOne(
                        RecordsQuery.create {
                            withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                            withQuery(
                                Predicates.and(
                                    Predicates.eq("engine", it.engine),
                                    Predicates.eq("elementId", it.taskId?.toString())
                                )
                            )
                        }
                    )
                    if (existingElement == null) {
                        createTaskElement(it, true)
                    } else {
                        val data = ObjectData.create()
                        data["completed"] = it.time
                        data["outcome"] = it.outcome
                        data["outcomeName"] = it.outcomeName
                        data["comment"] = it.comment
                        recordsService.mutate(existingElement, data)
                    }
                }
            }
        }

        eventsService.addListener<FlowElementEvent> {
            withEventType(BPMN_EVENT_FLOW_ELEMENT_START)
            withDataClass(FlowElementEvent::class.java)
            withAction { event ->
                execPostTxnAction(event) {
                    createFlowElement(it)
                }
            }
        }
    }

    private fun <T : Any> execPostTxnAction(arg: T, action: (T) -> Unit) {
        val ctx = RequestContext.getCurrentNotNull()
        if (ctx.ctxData.txnId != null && ctx.ctxData.txnOwner) {
            RequestContext.doAfterCommit {
                action.invoke(arg)
            }
        } else {
            action.invoke(arg)
        }
    }

    private fun createTaskElement(event: UserTaskEvent, completedEvent: Boolean) {
        log.debug { "Create task element. Event: $event. Completed: $completedEvent" }
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

    private fun createFlowElement(event: FlowElementEvent) {
        if (event.elementType == "UserTask") {
            log.debug { "User task flow take skipped: $event" }
            return
        }
        log.debug { "Create BPMN element by event: $event" }
        val data = ObjectData.create(event)
        data["created"] = event.time
        data["completed"] = event.time
        if (data["engine"].asText().isBlank()) {
            data["engine"] = "flowable"
        }
        recordsService.create(BPMN_ELEMENTS_REPO_SOURCE_ID, data)
    }
}
