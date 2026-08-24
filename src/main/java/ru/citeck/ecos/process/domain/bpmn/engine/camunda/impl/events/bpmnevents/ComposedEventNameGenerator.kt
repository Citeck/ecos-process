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

        val result = mutableListOf<ComposedEventName>()

        for (composedName in composedNamesOf(event.eventName)) {
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
        }

        return result
    }

    /**
     * A subscription of a predefined event type is named by the type, but the same event may also be caught
     * in manual mode by the platform event name typed by hand: it is the only way to catch an event without
     * a predefined type, and definitions written that way stay deployed after the type is added. So an
     * incoming event of a predefined type resolves under both names.
     */
    private fun composedNamesOf(eventName: String): Set<String> {
        val foundEventType = EcosEventType.from(eventName)
        if (foundEventType == EcosEventType.UNDEFINED || foundEventType == EcosEventType.USER_EVENT) {
            return setOf(eventName)
        }
        return linkedSetOf(foundEventType.name, eventName)
    }
}

data class IncomingEventData(
    val eventName: String,
    val record: EntityRef = EntityRef.EMPTY,
    val recordType: EntityRef = EntityRef.EMPTY,
    val workspace: String = ModelUtils.DEFAULT_WORKSPACE_ID
)
