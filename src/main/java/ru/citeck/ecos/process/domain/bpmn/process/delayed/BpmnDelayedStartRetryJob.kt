package ru.citeck.ecos.process.domain.bpmn.process.delayed

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.BpmnEventSubscriptionService

@Component
@Profile("!test")
class BpmnDelayedStartRetryJob(
    private val delayedStartService: BpmnDelayedStartService,
    private val eventSubscriptionService: BpmnEventSubscriptionService
) {

    @Scheduled(fixedDelayString = "\${ecos-process.bpmn.async-start-process.delayed-retry.job-rate-ms}")
    fun execute() {
        if (!eventSubscriptionService.isInitialized()) {
            return
        }
        delayedStartService.processDelayedCommands()
    }
}
