package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate

object EventSubscriptionCombiner {

    const val EVENT_ID_ATT = "\$event.id"
    const val EVENT_TIME_ATT = "\$event.time"
    const val EVENT_TYPE_ATT = "\$event.type"
    const val EVENT_USER_ATT = "\$event.user"
    const val EVENT_RECORD_ID_ATT = "${EcosEventType.RECORD_ATT}?id"
    const val EVENT_RECORD_TYPE_ID_ATT = "${EcosEventType.RECORD_ATT}.${RecordConstants.ATT_TYPE}?id"

    val DEFAULT_ATTS = listOf(
        EVENT_ID_ATT,
        EVENT_TIME_ATT,
        EVENT_TYPE_ATT,
        EVENT_USER_ATT,
        EVENT_RECORD_ID_ATT,
        EVENT_RECORD_TYPE_ID_ATT,
    )

    /**
     * Combine subscriptions with same event name.
     * Model will be merged by model values and predicate atts.
     * Default models added from [EcosEventType] and [DEFAULT_ATTS].
     */
    fun combine(subscriptions: List<EventSubscription>): List<CombinedEventSubscription> {

        val mapping = mutableMapOf<String, MutableSet<String>>()

        for (subscription in subscriptions) {
            val eventName = subscription.name.event
            val modelAttributes = subscription.model.values

            val predicate: Predicate = Json.mapper.read(subscription.predicate, Predicate::class.java)
                ?: VoidPredicate.INSTANCE
            val attsFromPredicate = PredicateUtils.getAllPredicateAttributes(predicate)

            val allAtts = modelAttributes + attsFromPredicate

            mapping.computeIfAbsent(eventName) { mutableSetOf() }.addAll(allAtts)
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
