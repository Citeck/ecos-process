package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

object ComposedEventNameGenerator {

    fun generateFromIncomingEcosEvent(
        event: IncomingEventData
    ): List<ComposedEventName> {
        if (event.eventName.isBlank()) {
            return emptyList()
        }

        val composedName = let {
            val foundEventType = EcosEventType.from(event.eventName)
            if (foundEventType == EcosEventType.UNDEFINED || foundEventType == EcosEventType.USER_EVENT) {
                event.eventName
            } else {
                foundEventType.name
            }
        }

        val result = mutableListOf<ComposedEventName>()

        result.add(ComposedEventName(composedName))

        if (event.recordType != EntityRef.EMPTY) {
            result.add(ComposedEventName(composedName, ComposedEventName.RECORD_ANY, event.recordType.toString()))
        }

        if (event.record != EntityRef.EMPTY) {
            result.add(ComposedEventName(composedName, event.record.toString()))
            if (event.recordType != EntityRef.EMPTY) {
                result.add(ComposedEventName(composedName, event.record.toString(), event.recordType.toString()))
            }
        }

        return result
    }
}

data class IncomingEventData(
    val eventName: String,
    val record: EntityRef = EntityRef.EMPTY,
    val recordType: EntityRef = EntityRef.EMPTY,
    val workspace: String = ModelUtils.DEFAULT_WORKSPACE_ID
)
