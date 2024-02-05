package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.conditional

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventExploder
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

@Component
class BpmnConditionalEventsProcessor(
    private val camundaEventExploder: CamundaEventExploder,
    private val camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }


    fun processEvent(updated: RecordUpdatedEvent) {
        val time = measureTimeMillis {
            if (updated.record == null || updated.changed == null) {
                return
            }

            camundaEventSubscriptionFinder.getActualCamundaConditionalEventsForBusinessKey(updated.record.toString())
                .filter { conditionalEvent ->
                    conditionalEvent.event.reactOnDocumentChange &&
                        conditionalEvent.event.documentVariables.isEmpty() ||
                        conditionalEvent.event.documentVariables.any {
                            it in updated.changed
                        }
                }
                .forEach {
                    camundaEventExploder.fireConditionalEvent(it.id)
                }
        }

        log.trace { "Processed conditional events for record ${updated.record} in $time ms" }
    }
}

data class RecordUpdatedEvent(
    @AttName("record?id")
    val record: EntityRef? = null,
    @AttName("diff.list[].id")
    val changed: List<String>? = null
)
