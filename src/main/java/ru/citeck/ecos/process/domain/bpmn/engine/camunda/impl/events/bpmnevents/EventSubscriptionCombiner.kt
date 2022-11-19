package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

object EventSubscriptionCombiner {

    /**
     * Combine subscriptions with same event name.
     * Model will be merged by values.
     */
    fun combine(subscriptions: List<EventSubscription>): List<CombinedEventSubscription> {

        val mapping = mutableMapOf<String, MutableSet<String>>()

        for (subscription in subscriptions) {
            val eventName = subscription.name.event
            val values = subscription.model.values

            mapping.computeIfAbsent(eventName) { mutableSetOf() }.addAll(values)
        }

        return mapping.map { (eventName, values) ->
            CombinedEventSubscription(
                eventName = eventName,
                attributes = values
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
