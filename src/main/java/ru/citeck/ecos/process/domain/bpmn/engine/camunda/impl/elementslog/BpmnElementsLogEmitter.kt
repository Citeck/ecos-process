package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.elementslog

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.process.domain.bpmn.elements.BPMN_EVENT_FLOW_ELEMENT_START
import ru.citeck.ecos.process.domain.bpmn.elements.BPMN_EVENT_USER_TASK_COMPLETE
import ru.citeck.ecos.process.domain.bpmn.elements.BPMN_EVENT_USER_TASK_CREATE
import ru.citeck.ecos.process.domain.bpmn.elements.dto.FlowElementEvent
import ru.citeck.ecos.process.domain.bpmn.elements.dto.TaskElementEvent

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnElementsLogEmitter(
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
        EmitterConfig.create<TaskElementEvent> {
            source = appName
            eventType = BPMN_EVENT_USER_TASK_CREATE
            eventClass = TaskElementEvent::class.java
        }
    )

    private val userTaskCompleteEmitter = eventsService.getEmitter(
        EmitterConfig.create<TaskElementEvent> {
            source = appName
            eventType = BPMN_EVENT_USER_TASK_COMPLETE
            eventClass = TaskElementEvent::class.java
        }
    )

    fun emitElementStart(event: FlowElementEvent) {
        flowElementsStartEmitter.emit(event)
    }

    fun emitTaskCreateEvent(event: TaskElementEvent) {
        userTaskCreateEmitter.emit(event)
    }

    fun emitTaskCompleteEvent(event: TaskElementEvent) {
        userTaskCompleteEmitter.emit(event)
    }
}
