package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

object EventSubscriptionCombiner {

    val DEFAULT_ATTS = listOf(
        "\$event.id",
        "\$event.time",
        "\$event.type",
        "\$event.user"
    )

    /**
     * Combine subscriptions with same event name.
     * Model will be merged by values.
     * Default models added from [EcosEventType] and [DEFAULT_ATTS].
     */
    fun combine(subscriptions: List<EventSubscription>): List<CombinedEventSubscription> {

        val mapping = mutableMapOf<String, MutableSet<String>>()

        for (subscription in subscriptions) {
            val eventName = subscription.name.event
            val values = subscription.model.values

            mapping.computeIfAbsent(eventName) { mutableSetOf() }.addAll(values)
        }

        return mapping.map { (eventName, atts) ->

            atts.addAll(DEFAULT_ATTS)

            val defaultAttsForEvent = EcosEventType.from(eventName).eventRepresentations.map {
                it.defaultModel.values
            }.flatten()
            atts.addAll(defaultAttsForEvent)

            CombinedEventSubscription(
                eventName = eventName,
                attributes = atts
            )
        }
    }
}

data class CombinedEventSubscription(
    val eventName: String,
    val attributes: Set<String>,
)

fun List<EventSubscription>.combine(): List<CombinedEventSubscription> {
    return EventSubscriptionCombiner.combine(this)
}
