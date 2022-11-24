package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData

@Component
class CamundaEventProcessor {

    fun processEvent(event: ObjectData) {

        println("Received event: \n$event")
    }
}
