package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional

import org.camunda.bpm.engine.impl.core.variable.event.VariableEvent
import org.camunda.bpm.engine.impl.interceptor.Command
import org.camunda.bpm.engine.impl.interceptor.CommandContext

class FireConditionalEventCmd(
    private val signalId: String
) : Command<Unit> {

    override fun execute(commandContext: CommandContext) {

        val eventSubscriptionManager = commandContext.eventSubscriptionManager
        val subs = eventSubscriptionManager.findEventSubscriptionById(signalId)

        val stubEvent = VariableEvent(ConditionalVariableInstanceStub, null, null)

        subs.eventReceived(stubEvent, false)
    }
}
