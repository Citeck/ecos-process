package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.process.common.toPrettyString

@Component("events")
class CamundaProcessEcosEventsService(
    private val eventsService: EventsService,

    @Value("\${spring.application.name}")
    private val appName: String
) : CamundaProcessEngineService {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val KEY = "events"
    }

    override fun getKey(): String {
        return KEY
    }

    fun send(type: String, data: BpmnDataValue) {
        log.debug { "Send ecos event: $type \n$${data.toPrettyString()}" }

        eventsService.getEmitter(
            EmitterConfig.create<DataValue> {
                source = appName
                eventType = type
                eventClass = DataValue::class.java
            }
        ).emit(data.asDataValue())
    }
}
