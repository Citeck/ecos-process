package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.EventType
import ru.citeck.ecos.webapp.api.entity.EntityRef

object ComposedEventNameGenerator {

    fun generateFromIncomingEcosEvent(
        event: IncomingEventData
    ): List<ComposedEventName> {
        if (event.name.isBlank()) {
            return emptyList()
        }

        val composedName = let {
            val foundEventType = EventType.from(event.name)
            if (foundEventType == EventType.UNDEFINED) {
                event.name
            } else {
                foundEventType.name
            }
        }

        val result = mutableListOf<ComposedEventName>()

        result.add(ComposedEventName(composedName, COMPOSED_EVENT_NAME_DOCUMENT_ANY))

        if (event.type != EntityRef.EMPTY) {
            result.add(ComposedEventName(composedName, COMPOSED_EVENT_NAME_DOCUMENT_ANY, event.type.toString()))
        }

        if (event.document != EntityRef.EMPTY) {
            result.add(ComposedEventName(composedName, event.document.toString()))
            if (event.type != EntityRef.EMPTY) {
                result.add(ComposedEventName(composedName, event.document.toString(), event.type.toString()))
            }
        }

        return result
    }
}

data class IncomingEventData(
    val name: String,
    val document: EntityRef = EntityRef.EMPTY,
    val type: EntityRef = EntityRef.EMPTY
)
