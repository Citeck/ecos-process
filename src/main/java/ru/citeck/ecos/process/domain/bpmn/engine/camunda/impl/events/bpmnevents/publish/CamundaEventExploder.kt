package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import mu.KotlinLogging
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional.FireConditionalEventCmd

@Component
class CamundaEventExploder(
    private val camundaCommandExecutor: CommandExecutor
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun fireEvent(signalId: String, event: BpmnDataValue) {
        log.debug { "Fire signal $signalId with data \n${Json.mapper.toPrettyString(event)}" }

        val signal = SendSignalEventByIdCmd(signalId, event)
        camundaCommandExecutor.execute(signal)
    }

    fun fireConditionalEvent(eventId: String) {
        log.debug { "Fire conditional event $eventId" }

        val signal = FireConditionalEventCmd(eventId)
        camundaCommandExecutor.execute(signal)
    }
}
