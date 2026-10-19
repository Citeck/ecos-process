package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity
import org.camunda.bpm.engine.impl.scripting.ExecutableScript
import ru.citeck.ecos.micrometer.obs.EcosObsContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.IncomingEventData
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent

sealed class BpmnObsCtx(name: String) : EcosObsContext(name) {

    class EventProcess(
        val event: GeneralEvent
    ) : BpmnObsCtx("ecos.bpmn.event.process")

    class SubscriptionsFind(
        val eventData: IncomingEventData
    ) : BpmnObsCtx("ecos.bpmn.event.subscriptions.find")

    class DeploymentResolve(
        val subscription: EventSubscriptionEntity
    ) : BpmnObsCtx("ecos.bpmn.event.deployment.resolve")

    class Predicate(
        val subscription: CamundaEventSubscription
    ) : BpmnObsCtx("ecos.bpmn.event.predicate")

    class Signal(
        val subscriptionId: String
    ) : BpmnObsCtx("ecos.bpmn.event.signal")

    class ConditionalFire(
        val subscriptionId: String
    ) : BpmnObsCtx("ecos.bpmn.conditional.fire")

    class Query(
        val statement: String
    ) : BpmnObsCtx("ecos.bpmn.event.subscriptions.query")

    class Script(
        val script: ExecutableScript,
        val scope: VariableScope?
    ) : BpmnObsCtx("ecos.bpmn.script.execute")

    class Definition(
        val phase: DefinitionPhase,
        val deploymentId: String,
        val activityId: String? = null
    ) : BpmnObsCtx(phase.observationName)

    enum class DefinitionPhase(val observationName: String) {
        RESOLVE("ecos.bpmn.event.definition.resolve"),
        LOAD("ecos.bpmn.event.definition.load"),
        CONDITIONAL_RESOLVE("ecos.bpmn.conditional.definition.resolve"),
        CONDITIONAL_LOAD("ecos.bpmn.conditional.definition.load")
    }
}
