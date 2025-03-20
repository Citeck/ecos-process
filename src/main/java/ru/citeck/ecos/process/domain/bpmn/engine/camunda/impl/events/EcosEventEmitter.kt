package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.EcosUserTaskEvent

@Component
class EcosEventEmitter(
    eventsService: EventsService,

    @Value("\${spring.application.name}")
    private val appName: String
) {

    companion object {
        const val ECOS_EVENT_USER_TASK_CREATE = "ecos-user-task-create"
    }

    private val ecosUserTaskCreateEmitter = eventsService.getEmitter(
        EmitterConfig.create<EcosUserTaskEvent> {
            source = appName
            eventType = ECOS_EVENT_USER_TASK_CREATE
            eventClass = EcosUserTaskEvent::class.java
        }
    )

    fun emitEcosUserTaskCreateEvent(event: EcosUserTaskEvent) {
        ecosUserTaskCreateEmitter.emit(event)
    }
}
