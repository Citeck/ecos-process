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
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_BUSINESS_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.deleteAllProcDefinitions
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
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
        deleteAllProcDefinitions()
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
        camundaEventSubscriptionFinder.findAllDeployedSubscriptions()

        assertThat(cache).hasSize(2)
    }

    @Test
    fun `find all deployed subscriptions with different versions payload`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdModified = "test-subscriptions-start-signal-event_2"

        saveAndDeployBpmn(SUBSCRIPTION, procId)
        saveAndDeployBpmn(SUBSCRIPTION, procIdModified)

        val eventSubscription = EventSubscription(
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name,
                record = ComposedEventName.RECORD_ANY
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

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()

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

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "${EcosEventType.COMMENT_CREATE.name};\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of signal boundary event with current document`() {
        val procId = "test-subscriptions-boundary-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "${EcosEventType.COMMENT_CREATE.name};\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of end signal event`() {
        val procId = "test-subscriptions-end-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "some-signal;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of throw signal event`() {
        val procId = "test-subscriptions-throw-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "some-signal;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap()
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of start signal events with event sub process`() {
        val procId = "test-subscriptions-start-signals-event-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()

        assertThat(foundSubscription).hasSize(2)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            EventSubscription(
                name = "start-1;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
            ),
            EventSubscription(
                name = "start-2;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of pool participant with hierarchy sub process`() {
        val procId = "test-subscriptions-pool-participants-with-hierarchy-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
        val eventSubscription = EventSubscription(
            name = "signal-1;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
            model = emptyMap(),
            predicate = null
        )

        assertThat(foundSubscription).hasSize(4)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            eventSubscription,
            eventSubscription.copy(name = "signal-2;ANY".toComposedEventName()),
            eventSubscription.copy(name = "signal-3;\${$VAR_BUSINESS_KEY}".toComposedEventName()),
            eventSubscription.copy(name = "signal-4;\${$VAR_BUSINESS_KEY}".toComposedEventName())
        )
    }

    private fun String.toComposedEventName(): ComposedEventName {
        return ComposedEventName.fromString(this)
    }
}
