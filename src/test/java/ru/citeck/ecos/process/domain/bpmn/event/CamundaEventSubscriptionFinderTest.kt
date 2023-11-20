package ru.citeck.ecos.process.domain.bpmn.event

import com.hazelcast.core.HazelcastInstance
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_BUSINESS_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.cleanDefinitions
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

const val SUBSCRIPTION = "subscription"

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CamundaEventSubscriptionFinderTest {

    @Autowired
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    @Autowired
    private lateinit var hazelCast: HazelcastInstance

    @AfterEach
    fun clearSubscriptions() {
        cleanDefinitions()
    }

    @Test
    fun `call find all deployed subscriptions should warmup cache`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdModified = "test-subscriptions-start-signal-event_2"

        val cache = hazelCast.getMap<String, EventSubscription>(
            BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME
        )
        cache.clear()

        assertThat(cache).hasSize(0)

        saveAndDeployBpmn(SUBSCRIPTION, procId)
        saveAndDeployBpmn(SUBSCRIPTION, procIdModified)
        camundaEventSubscriptionFinder.findDeployedSubscriptionsData()

        assertThat(cache).hasSize(2)
    }

    @Test
    fun `find all deployed subscriptions with different versions payload`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdModified = "test-subscriptions-start-signal-event_2"

        saveAndDeployBpmn(SUBSCRIPTION, procId)
        saveAndDeployBpmn(SUBSCRIPTION, procIdModified)

        val eventSubscription = EventSubscription(
            elementId = "startEvent",
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name
            ),
            model = mapOf(
                "keyFoo" to "valueFoo",
                "keyBar" to "valueBar"
            ),
            predicate = """
                {
                    "att": "event.statusBefore",
                    "val": "approval",
                    "t": "eq"
                }
            """.trimIndent()
        )

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(2)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            eventSubscription,
            eventSubscription.copy(
                model = mapOf(
                    "keyFoo" to "valueFoo",
                    "keyBar" to "valueBar2"
                )
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of signal boundary non interrupting event with current document`() {
        val procId = "test-subscriptions-boundary-non-interrupting-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                elementId = "event_boundary",
                name = "${EcosEventType.COMMENT_CREATE.name};\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
                model = emptyMap(),
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of signal boundary event with current document`() {
        val procId = "test-subscriptions-boundary-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                elementId = "boundary_event",
                name = "${EcosEventType.COMMENT_CREATE.name};\${$BPMN_BUSINESS_KEY};ANY;pr_0"
                    .toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of end signal event`() {
        val procId = "test-subscriptions-end-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                elementId = "event_end",
                name = "some-signal;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of throw signal event`() {
        val procId = "test-subscriptions-throw-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                elementId = "throw_event",
                name = "some-signal;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of start signal events with event sub process`() {
        val procId = "test-subscriptions-start-signals-event-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(2)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            EventSubscription(
                elementId = "event_start_1",
                name = "start-1;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
                model = emptyMap(),
            ),
            EventSubscription(
                elementId = "event_start_2",
                name = "start-2;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
                model = emptyMap(),
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of pool participant with hierarchy sub process`() {
        val procId = "test-subscriptions-pool-participants-with-hierarchy-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()
        val foundSubscription = subscriptionsData.subscriptions

        val eventSubscription = EventSubscription(
            elementId = "event_signal_1",
            name = "signal-1;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName(),
            model = emptyMap(),
            predicate = null
        )

        assertThat(subscriptionsData.conditionalEventsEcosTypes).hasSize(0)
        assertThat(foundSubscription).hasSize(4)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            eventSubscription,
            eventSubscription.copy(
                elementId = "event_signal_2",
                name = "signal-2;ANY;ANY;pr_0".toComposedEventName()
            ),
            eventSubscription.copy(
                elementId = "event_signal_3",
                name = "signal-3;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName()
            ),
            eventSubscription.copy(
                elementId = "event_signal_4",
                name = "signal-4;\${$BPMN_BUSINESS_KEY};ANY;pr_0".toComposedEventName()
            )
        )
    }

    @Test
    fun `check conditional events ecos types in deployed subscriptions data `() {
        val procId = "test-conditional-event-subscriptions"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val subscriptionsData = camundaEventSubscriptionFinder.findDeployedSubscriptionsData()

        assertThat(subscriptionsData.subscriptions).hasSize(0)
        assertThat(subscriptionsData.conditionalEventsEcosTypes).containsExactlyInAnyOrder(
            EntityRef.valueOf("emodel/type@type_1"),
            EntityRef.valueOf("emodel/type@type_2"),
            EntityRef.valueOf("emodel/type@type_3"),
            EntityRef.valueOf("emodel/type@type_4"),
        )
    }

    private fun String.toComposedEventName(): ComposedEventName {
        return ComposedEventName.fromString(this)
    }
}
