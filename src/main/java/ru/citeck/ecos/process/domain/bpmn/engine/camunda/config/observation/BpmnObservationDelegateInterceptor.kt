package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation

import org.camunda.bpm.engine.impl.delegate.DelegateInvocation
import org.camunda.bpm.engine.impl.interceptor.DelegateInterceptor
import ru.citeck.ecos.micrometer.EcosMicrometerContext

/**
 * Keeps Camunda's configured interceptor responsible for authorization and execution context.
 * The observation includes listeners and synchronous downstream work invoked by a delegate.
 */
class BpmnObservationDelegateInterceptor(
    private val delegate: DelegateInterceptor,
    private val micrometerContext: EcosMicrometerContext
) : DelegateInterceptor {

    override fun handleInvocation(invocation: DelegateInvocation) {
        micrometerContext.createObs(BpmnObsCtx.Delegate(invocation)).observe {
            delegate.handleInvocation(invocation)
        }
    }
}
