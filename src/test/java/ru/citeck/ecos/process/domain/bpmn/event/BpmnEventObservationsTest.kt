package ru.citeck.ecos.process.domain.bpmn.event

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.impl.scripting.ScriptFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.micrometer.EcosMicrometerContext
import ru.citeck.ecos.micrometer.obs.EcosObs
import ru.citeck.ecos.micrometer.obs.EcosObsContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsConvention
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsCtx
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventExploder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventProcessor
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.predicate.PredicateService
import java.time.Instant

class BpmnEventObservationsTest {

    private val registry = ObservationRegistry.create()
    private val stopped = mutableListOf<Observation.Context>()
    private val meter = object : EcosMicrometerContext {
        override fun createObs(name: String): EcosObs {
            return createObs(EcosObsContext(name))
        }
        override fun createObs(context: EcosObsContext): EcosObs {
            return EcosObs(Observation.createNotStarted(context.name, { context }, registry))
        }
    }
    private val finder = mock<CamundaEventSubscriptionFinder>()
    private val exploder = mock<CamundaEventExploder>()
    private val processor = CamundaEventProcessor(exploder, finder, mock<PredicateService>()).apply {
        micrometerContext = meter
    }

    init {
        registry.observationConfig().observationConvention(BpmnObsConvention()).observationHandler(object : ObservationHandler<Observation.Context> {
            override fun supportsContext(context: Observation.Context): Boolean {
                return true
            }
            override fun onStop(context: Observation.Context) {
                stopped.add(context)
            }
        })
    }

    @Test
    fun emptySubscriptionsStillFinishObservation() {

        whenever(finder.getActualCamundaSubscriptions(any())).thenReturn(emptyList())
        val event = event()
        processor.processEvent(event)
        val span = stopped.single()
        assertSame(event, (span as BpmnObsCtx.EventProcess).event)
        assertEquals("ecos.bpmn.event.process", span.name)
        assertEquals("event-1", span.getHighCardinalityKeyValue("event.id")!!.value)
        verifyNoInteractions(exploder)
        assertNull(registry.currentObservation)
    }

    @Test
    fun attributesAndPredicateAreNestedWithoutPayloadTags() {

        whenever(finder.getActualCamundaSubscriptions(any())).thenReturn(
            listOf(
                CamundaEventSubscription(
                    "subscription-1",
                    EventSubscription("activity-1", ComposedEventName(event = "custom"), emptyMap())
                )
            )
        )
        processor.processEvent(event())
        verify(exploder).fireEvent(eq("subscription-1"), any())
        for (name in listOf("ecos.bpmn.event.attributes", "ecos.bpmn.event.predicate")) {
            assertEquals("ecos.bpmn.event.process", stopped.single { it.name == name }.parentObservation!!.contextView.name)
        }
        assertTrue(stopped.none { it.highCardinalityKeyValues.any { kv -> kv.value.contains("private-payload") } })
    }

    @Test
    fun subscriptionLookupFailureIsNotSwallowedAndScopeIsClosed() {

        val failure = IllegalStateException("query failed")
        whenever(finder.getActualCamundaSubscriptions(any())).thenThrow(failure)
        assertSame(failure, assertThrows(IllegalStateException::class.java) { processor.processEvent(event()) })
        assertSame(failure, stopped.single().error)
        assertNull(registry.currentObservation)
    }

    @Test
    fun scriptContextExportsExecutionIdentityWithoutSourceOrVariables() {

        val script = ScriptFactory().createScriptFromSource("javascript", "var secret = 'private-script-data';")
        val scope = mock<DelegateExecution> {
            on { processDefinitionId } doReturn "definition-1"
            on { currentActivityId } doReturn "activity-1"
        }
        val context = BpmnObsCtx.Script(script, scope)
        val tags = BpmnObsConvention().getHighCardinalityKeyValues(context)
        assertEquals(setOf("script.language", "process.definition.id", "activity.id"), tags.map { it.key }.toSet())
        assertFalse(tags.any { it.value.contains("private-script-data") })
        verify(scope).processDefinitionId
        verify(scope).currentActivityId
        verifyNoMoreInteractions(scope)
    }

    private fun event(): GeneralEvent {

        return GeneralEvent(
            "event-1",
            Instant.now(),
            "custom",
            "test-user",
            "default",
            mapOf("secret" to DataValue.create("private-payload"))
        )
    }
}
