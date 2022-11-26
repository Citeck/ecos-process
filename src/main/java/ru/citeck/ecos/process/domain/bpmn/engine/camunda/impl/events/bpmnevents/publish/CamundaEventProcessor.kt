package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.IncomingEventData
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.meta.RecordsTemplateService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class CamundaEventProcessor(
    private val exploder: CamundaEventExploder,
    private val eventSubscriptionFinder: CamundaEventSubscriptionFinder,
    private val predicateService: PredicateService,
    private val recordsTemplateService: RecordsTemplateService,
    private val recordsServiceFactory: RecordsServiceFactory,
    private val recordsService: RecordsService
) {

    fun processEvent(incomingEvent: GeneralEvent) {

        val incomingEventData = incomingEvent.toIncomingEventData()

        val foundSubscriptions = eventSubscriptionFinder.getActualCamundaSubscriptions(incomingEventData)

        //TODO: refactor after tests
        foundSubscriptions.forEach { subscription ->

            val finalAttributesMap = mutableMapOf<String, DataValue>()

            //TODO: hardcoded?
            val additionalMeta = mapOf("record" to incomingEventData.record)

            RequestContext.doWithCtx(
                recordsServiceFactory,
                { data ->
                    data.withCtxAtts(additionalMeta)
                }
            ) {
                recordsService.getAtts(incomingEvent.attributes, subscription.event.model).forEach { key, attr ->
                    finalAttributesMap[key] = attr
                }
            }

            val predicate = if (subscription.event.predicate.isNullOrBlank()) {
                VoidPredicate.INSTANCE
            } else {
                Json.mapper.read(subscription.event.predicate, Predicate::class.java)
            }

            val attributes = ObjectData.create(finalAttributesMap)

            if (predicate != null && predicate !is VoidPredicate) {
                val resolvedFilter = recordsTemplateService.resolve(
                    predicate,
                    RecordRef.create("meta", "")
                )

                val element = RecordAttsElement.create(RecordAtts(RecordRef.EMPTY, attributes))
                if (!predicateService.isMatch(element, resolvedFilter)) {
                    return@forEach
                }
            }

            exploder.fireEvent(subscription.id, attributes)
        }
    }
}


fun GeneralEvent.toIncomingEventData(): IncomingEventData {
    return IncomingEventData(
        eventName = this.type,
        record = EntityRef.valueOf(this.record),
        recordType = EntityRef.EMPTY
    )
}
