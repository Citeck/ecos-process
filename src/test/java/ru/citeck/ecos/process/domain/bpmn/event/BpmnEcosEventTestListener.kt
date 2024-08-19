package ru.citeck.ecos.process.domain.bpmn.event

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.events2.EventsService

@Component
class BpmnEcosEventTestListener(
    eventsService: EventsService,
    private val action: BpmnEcosEventTestAction
) {

    init {

        eventsService.addListener<ObjectData> {
            withEventType("ecos-event-script-task-throw-payload")
            withDataClass(ObjectData::class.java)
            withTransactional(true)
            withAttributes(
                mapOf("foo" to "foo", "itsNum" to "number")
            )
            withAction { event ->
                callAction(event)
            }
        }
    }

    private fun callAction(event: ObjectData) {
        action.callAction(event)
    }
}

@Component
class BpmnEcosEventTestAction {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun callAction(event: ObjectData) {
        log.info { "Test event received: $event" }
    }
}
