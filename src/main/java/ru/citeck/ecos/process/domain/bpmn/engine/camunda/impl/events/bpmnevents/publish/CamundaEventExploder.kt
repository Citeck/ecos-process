package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.micrometer.EcosMicrometerContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.observation.BpmnObsCtx
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional.FireConditionalEventCmd
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Component
class CamundaEventExploder(
    private val camundaCommandExecutor: CommandExecutor,
    private val procDefService: ProcDefService,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val bpmnIO: BpmnIO
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Autowired(required = false)
    internal var micrometerContext: EcosMicrometerContext = EcosMicrometerContext.NOOP

    fun fireEvent(signalId: String, event: BpmnDataValue) {
        return micrometerContext.createObs(BpmnObsCtx.Signal(signalId))
            .observe { fireEventImpl(signalId, event) }
    }

    private fun fireEventImpl(signalId: String, event: BpmnDataValue) {
        log.debug { "Fire signal $signalId with data \n${Json.mapper.toPrettyString(event)}" }

        val signal = SendSignalEventByIdCmd(
            signalId,
            event,
            procDefService,
            procDefRevDataProvider,
            bpmnIO,
            micrometerContext
        )
        camundaCommandExecutor.execute(signal)
    }

    fun fireConditionalEvent(eventId: String) {
        return micrometerContext.createObs(BpmnObsCtx.ConditionalFire(eventId))
            .observe { fireConditionalEventImpl(eventId) }
    }

    private fun fireConditionalEventImpl(eventId: String) {
        log.debug { "Fire conditional event $eventId" }

        val signal = FireConditionalEventCmd(eventId)
        camundaCommandExecutor.execute(signal)
    }
}
