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
     * Default models added specified by event name.
     */
    fun combine(subscriptions: List<EventSubscription>): List<CombinedEventSubscription> {

        val mapping = mutableMapOf<String, MutableSet<String>>()

        for (subscription in subscriptions) {
            val eventName = subscription.name.event
            val values = subscription.model.values

            mapping.computeIfAbsent(eventName) { mutableSetOf() }.addAll(values)
        }

        // TODO: add default atts to mode, add default atts by event name?

        return mapping.map { (eventName, atts) ->

            atts.addAll(DEFAULT_ATTS)

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
