package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation
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

    class SignalCommand(
        val subscriptionId: String,
        val phase: SignalPhase,
        val subscription: EventSubscriptionEntity? = null
    ) : BpmnObsCtx(phase.observationName)

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

    class Delegate(val invocation: DelegateInvocation) : BpmnObsCtx("ecos.bpmn.delegate.invoke") {

        private val execution = invocation.contextExecution as? DelegateExecution
        val processDefinitionId = execution?.processDefinitionId
        val activityId = execution?.currentActivityId
    }

    class ScriptPhase(
        val script: ExecutableScript,
        val scope: VariableScope?,
        val phase: ScriptStage
    ) : BpmnObsCtx(phase.observationName)

    enum class ScriptStage(val observationName: String) {
        ENGINE("ecos.bpmn.script.engine.resolve"),
        BINDINGS("ecos.bpmn.script.bindings.create"),
        ENVIRONMENT("ecos.bpmn.script.environment.execute"),
        EVALUATE("ecos.bpmn.script.evaluate"),
        DETACH("ecos.bpmn.script.result.detach")
    }

    class Definition(
        val phase: DefinitionPhase,
        val deploymentId: String,
        val activityId: String? = null
    ) : BpmnObsCtx(phase.observationName)

    enum class SignalPhase(val observationName: String) {
        COMMAND("ecos.bpmn.event.signal.command"),
        SUBSCRIPTIONS_FIND("ecos.bpmn.event.signal.subscriptions.find"),
        DEFINITIONS_RESOLVE("ecos.bpmn.event.signal.definitions.resolve"),
        AUTHORIZE_CATCH("ecos.bpmn.event.signal.authorize.catch"),
        AUTHORIZE_START("ecos.bpmn.event.signal.authorize.start"),
        NOTIFY_EXECUTIONS("ecos.bpmn.event.signal.executions.notify"),
        EXECUTION_CHECK("ecos.bpmn.event.signal.execution.check"),
        EVENT_RECEIVED("ecos.bpmn.event.signal.event.receive"),
        START_INSTANCES("ecos.bpmn.event.signal.instances.start"),
        INSTANCE_CREATE("ecos.bpmn.event.signal.instance.create"),
        INSTANCE_START("ecos.bpmn.event.signal.instance.start")
    }

    enum class DefinitionPhase(val observationName: String) {
        RESOLVE("ecos.bpmn.event.definition.resolve"),
        LOAD("ecos.bpmn.event.definition.load"),
        CONDITIONAL_RESOLVE("ecos.bpmn.conditional.definition.resolve"),
        CONDITIONAL_LOAD("ecos.bpmn.conditional.definition.load")
    }
}
