package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent

const val BPMN_EVENT_USER_TASK_CREATE = "bpmn-user-task-create"
const val BPMN_EVENT_USER_TASK_COMPLETE = "bpmn-user-task-complete"
const val BPMN_EVENT_USER_TASK_ASSIGN = "bpmn-user-task-assign"
const val BPMN_EVENT_FLOW_ELEMENT_START = "bpmn-flow-element-start"

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnEventEmitter(
    eventsService: EventsService,

    @Value("\${spring.application.name}")
    private val appName: String
) {

    private val flowElementsStartEmitter = eventsService.getEmitter(
        EmitterConfig.create<FlowElementEvent> {
            source = appName
            eventType = BPMN_EVENT_FLOW_ELEMENT_START
            eventClass = FlowElementEvent::class.java
        }
    )

    private val userTaskCreateEmitter = eventsService.getEmitter(
        EmitterConfig.create<UserTaskEvent> {
            source = appName
            eventType = BPMN_EVENT_USER_TASK_CREATE
            eventClass = UserTaskEvent::class.java
        }
    )

    private val userTaskCompleteEmitter = eventsService.getEmitter(
        EmitterConfig.create<UserTaskEvent> {
            source = appName
            eventType = BPMN_EVENT_USER_TASK_COMPLETE
            eventClass = UserTaskEvent::class.java
        }
    )

    private val userTaskAssignEmitter = eventsService.getEmitter(
        EmitterConfig.create<UserTaskEvent> {
            source = appName
            eventType = BPMN_EVENT_USER_TASK_ASSIGN
            eventClass = UserTaskEvent::class.java
        }
    )

    fun emitElementStart(event: FlowElementEvent) {
        flowElementsStartEmitter.emit(event)
    }

    fun emitUserTaskCreateEvent(event: UserTaskEvent) {
        userTaskCreateEmitter.emit(event)
    }

    fun emitUserTaskCompleteEvent(event: UserTaskEvent) {
        userTaskCompleteEmitter.emit(event)
    }

    fun emitUserTaskAssignEvent(event: UserTaskEvent) {
        userTaskAssignEmitter.emit(event)
    }
}
