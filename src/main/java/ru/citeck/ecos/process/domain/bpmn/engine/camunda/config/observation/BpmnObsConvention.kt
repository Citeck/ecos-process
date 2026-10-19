package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation

import io.micrometer.common.KeyValues
import io.micrometer.observation.Observation
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import ru.citeck.ecos.webapp.lib.spring.context.micrometer.GlobalEcosObsConvention

@Component
class BpmnObsConvention : GlobalEcosObsConvention<BpmnObsCtx> {

    override fun getLowCardinalityKeyValues(context: BpmnObsCtx): KeyValues {

        return if (context is BpmnObsCtx.Query) {
            KeyValues.of("statement", context.statement)
        } else {
            KeyValues.empty()
        }
    }

    override fun getHighCardinalityKeyValues(context: BpmnObsCtx): KeyValues {

        return when (context) {
            is BpmnObsCtx.EventProcess -> KeyValues.of("event.id", context.event.id, "event.type", context.event.type)
            is BpmnObsCtx.SubscriptionsFind -> KeyValues.of("event.type", context.eventData.eventName)
            is BpmnObsCtx.DeploymentResolve -> KeyValues.of("subscription.id", context.subscription.id)
            is BpmnObsCtx.Predicate -> KeyValues.of("subscription.id", context.subscription.id)
            is BpmnObsCtx.SignalCommand -> KeyValues.of("subscription.id", context.subscriptionId)
            is BpmnObsCtx.Signal -> KeyValues.of("subscription.id", context.subscriptionId)
            is BpmnObsCtx.ConditionalFire -> KeyValues.of("subscription.id", context.subscriptionId)
            is BpmnObsCtx.Definition -> getDefinitionKeyValues(context)
            is BpmnObsCtx.Script -> getScriptKeyValues(context)
            is BpmnObsCtx.ScriptPhase -> getScriptKeyValues(BpmnObsCtx.Script(context.script, context.scope))
            is BpmnObsCtx.Delegate -> getDelegateKeyValues(context)
            is BpmnObsCtx.Query -> KeyValues.empty()
        }
    }

    private fun getDefinitionKeyValues(context: BpmnObsCtx.Definition): KeyValues {

        val values = KeyValues.of("deployment.id", context.deploymentId)

        return if (context.activityId == null) {
            values
        } else {
            values.and("activity.id", context.activityId)
        }
    }

    private fun getScriptKeyValues(context: BpmnObsCtx.Script): KeyValues {

        var values = KeyValues.of("script.language", context.script.language)
        val execution = context.scope as? DelegateExecution

        execution?.processDefinitionId?.let {
            values = values.and("process.definition.id", it)
        }
        execution?.currentActivityId?.let {
            values = values.and("activity.id", it)
        }

        return values
    }

    private fun getDelegateKeyValues(context: BpmnObsCtx.Delegate): KeyValues {

        val invocation = context.invocation
        var values = KeyValues.of("invocation.type", invocation.javaClass.simpleName)
        context.processDefinitionId?.let {
            values = values.and("process.definition.id", it)
        }
        context.activityId?.let {
            values = values.and("activity.id", it)
        }
        return values
    }

    override fun supportsContext(context: Observation.Context): Boolean {
        return context is BpmnObsCtx
    }
}
