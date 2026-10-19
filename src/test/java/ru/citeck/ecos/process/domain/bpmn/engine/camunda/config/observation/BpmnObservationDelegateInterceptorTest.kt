package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation
import org.camunda.bpm.engine.impl.interceptor.DelegateInterceptor
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.micrometer.EcosMicrometerContext
import ru.citeck.ecos.micrometer.obs.EcosObs
import ru.citeck.ecos.micrometer.obs.EcosObsContext

class BpmnObservationDelegateInterceptorTest {

    @Test
    fun preservesConfiguredInterceptorResultsErrorsAndParentScopes() {

        val registry = ObservationRegistry.create()
        val stopped = mutableListOf<Observation.Context>()
        registry.observationConfig().observationConvention(BpmnObsConvention())
            .observationHandler(object : ObservationHandler<Observation.Context> {
                override fun supportsContext(context: Observation.Context): Boolean {
                    return true
                }
                override fun onStop(context: Observation.Context) {
                    stopped.add(context)
                }
            })
        val meter = object : EcosMicrometerContext {
            override fun createObs(name: String): EcosObs {
                return createObs(EcosObsContext(name))
            }
            override fun createObs(context: EcosObsContext): EcosObs {
                return EcosObs(Observation.createNotStarted(context.name, { context }, registry))
            }
        }
        var calls = 0
        val configured = DelegateInterceptor {
            calls++
            assertThat(registry.currentObservation?.context).isInstanceOf(BpmnObsCtx.Delegate::class.java)
            it.proceed()
        }
        val interceptor = BpmnObservationDelegateInterceptor(configured, meter)
        val failure = IllegalStateException("test failure")
        val parent = Observation.start("event.receive", registry)
        parent.openScope().use {
            val successful = object : DelegateInvocation(null, null) {
                override fun invoke() {
                    invocationResult = "result"
                }
            }
            interceptor.handleInvocation(successful)
            assertThat(successful.invocationResult).isEqualTo("result")
            val broken = object : DelegateInvocation(null, null) {
                override fun invoke() {
                    throw failure
                }
            }
            assertSame(failure, assertThrows(IllegalStateException::class.java) { interceptor.handleInvocation(broken) })
            assertSame(parent, registry.currentObservation)
        }
        parent.stop()
        assertThat(calls).isEqualTo(2)
        val delegates = stopped.filterIsInstance<BpmnObsCtx.Delegate>()
        assertThat(delegates).hasSize(2)
        assertSame(parent, delegates[0].parentObservation)
        assertSame(failure, delegates[1].error)
        assertThat(registry.currentObservation).isNull()
    }

    @Test
    fun capturesEntryActivityBeforeSynchronousContinuationChangesExecution() {

        val execution = Mockito.mock(DelegateExecution::class.java)
        Mockito.`when`(execution.processDefinitionId).thenReturn("process:24:deployment")
        Mockito.`when`(execution.currentActivityId).thenReturn("calendar-send")
        val invocation = object : DelegateInvocation(execution, null) {
            override fun invoke() {
                Mockito.`when`(execution.currentActivityId).thenReturn("next-activity")
            }
        }
        val context = BpmnObsCtx.Delegate(invocation)
        invocation.proceed()
        val tags = BpmnObsConvention().getHighCardinalityKeyValues(context)

        assertThat(tags.toList().associate { it.key to it.value }).containsEntry("activity.id", "calendar-send")
        assertThat(BpmnObsConvention().getLowCardinalityKeyValues(context)).isEmpty()
        Mockito.verify(execution, Mockito.never()).getVariables()
    }
}
