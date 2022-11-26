package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CombinedEventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscriptionCombiner

class EventSubscriptionCombinerTest {

    @Test
    fun `combine event subscriptions`() {
        val subscriptions = listOf(
            EventSubscription(
                name = ComposedEventName("event1", "doc1", "type1"),
                model = mapOf(
                    "key1" to "value1",
                    "key2" to "value2"
                )
            ),
            EventSubscription(
                name = ComposedEventName("event1", "doc2", "type1"),
                model = mapOf(
                    "key1" to "value1",
                    "key2" to "value2_1",
                )
            ),
            EventSubscription(
                name = ComposedEventName("event1", "doc1"),
                model = mapOf(
                    "key1" to "value1_1",
                    "key3" to "value3",
                )
            ),
            EventSubscription(
                name = ComposedEventName("event2", "doc1"),
                model = mapOf(
                    "key1" to "value1",
                    "key2" to "value2",
                )
            ),
            EventSubscription(
                name = ComposedEventName("event2", "doc2", "type1"),
                model = emptyMap()
            ),
            EventSubscription(
                name = ComposedEventName("event3", "doc2", "type1"),
                model = emptyMap()
            )
        )

        val combined = EventSubscriptionCombiner.combine(subscriptions)

        assertThat(combined).hasSize(3)
        assertThat(combined).containsExactlyInAnyOrder(
            CombinedEventSubscription(
                eventName = "event1",
                attributes = mutableSetOf(
                    "value1",
                    "value1_1",
                    "value2",
                    "value2_1",
                    "value3"
                ).addDefaultEventAtts().toSet()
            ),
            CombinedEventSubscription(
                eventName = "event2",
                attributes = mutableSetOf(
                    "value1",
                    "value2"
                ).addDefaultEventAtts().toSet()
            ),
            CombinedEventSubscription(
                eventName = "event3",
                attributes = EventSubscriptionCombiner.DEFAULT_ATTS.toSet()
            )
        )
    }
}
