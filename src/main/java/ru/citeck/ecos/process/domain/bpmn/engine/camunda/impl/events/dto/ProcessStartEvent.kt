package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef

data class ProcessStartEvent(
    val processKey: String,
    val processInstanceId: String,
    val processDefinitionId: String,
    val document: EntityRef
) {

    /**
     * Alias of [document] under the name a subscription narrows by: `ComposedEventNameGenerator` scopes an
     * incoming event with `record?id` / `record._type?id` (`EventSubscriptionCombiner.DEFAULT_ATTS`).
     * `UserTaskEvent` carries the subject under both names for the same reason.
     */
    val record: EntityRef
        get() = document
}
