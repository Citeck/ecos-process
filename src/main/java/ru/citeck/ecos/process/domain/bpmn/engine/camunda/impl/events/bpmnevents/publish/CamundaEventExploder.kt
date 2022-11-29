package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import mu.KotlinLogging
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData

@Component
class CamundaEventExploder(
    private val camundaCommandExecutor: CommandExecutor
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun fireEvent(signalId: String, event: ObjectData) {
        log.debug { "Fire signal $signalId with data $event" }

        val signal = SignalEventByIdCmd(signalId, event)
        camundaCommandExecutor.execute(signal)
    }
}