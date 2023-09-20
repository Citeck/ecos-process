package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional

import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventExploder
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnConditionalEventsListener(
    eventsService: EventsService,
    bpmnConditionalEventsProcessor: BpmnConditionalEventsProcessor
) {

    init {
        eventsService.addListener<RecordUpdatedEvent> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdatedEvent::class.java)
            withTransactional(true)
            withAction { updated ->

                bpmnConditionalEventsProcessor.processEvent(updated)

            }
        }
    }

}

@Component
class BpmnConditionalEventsProcessor(
    private val camundaEventExploder: CamundaEventExploder,
    private val camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder
) {

    fun processEvent(updated: RecordUpdatedEvent) {

        camundaEventSubscriptionFinder.getActualCamundaConditionalEventsForBusinessKey(updated.record.toString())
            .filter { conditionalEvent ->
                conditionalEvent.event.documentVariables.isEmpty() || conditionalEvent.event.documentVariables.any {
                    it in updated.changed
                }
            }
            .forEach {
                camundaEventExploder.fireConditionalEvent(it.id)
            }
    }

}

data class RecordUpdatedEvent(
    @AttName("record?id")
    val record: EntityRef,
    @AttName("diff.list[].id")
    val changed: List<String> = emptyList()
)
