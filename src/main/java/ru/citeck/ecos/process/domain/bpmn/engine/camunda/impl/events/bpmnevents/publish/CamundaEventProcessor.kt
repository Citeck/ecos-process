package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
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
        if (foundSubscriptions.isEmpty()) {
            return
        }

        val defaultModelForIncomingEvent = let {
            if (foundSubscriptions.isEmpty()) {
                emptyMap()
            } else {
                EcosEventType.findRepresentation(incomingEventData.eventName)?.defaultModel ?: emptyMap()
            }
        }

        val additionalMeta = mapOf(
            "record" to incomingEventData.record,
            "event" to mapOf(
                "id" to incomingEvent.id,
                "type" to incomingEvent.type,
                "time" to incomingEvent.time,
                "user" to incomingEvent.user
            )
        )

        for (subscription in foundSubscriptions) {
            val finalEventModel = mutableMapOf<String, String>()
            finalEventModel.putAll(defaultModelForIncomingEvent)
            finalEventModel.putAll(subscription.event.model)

            val eventAttributes = evaluateAttributes(incomingEvent.attributes, finalEventModel, additionalMeta)
            if (eventAttributes.isMatchPredicates(subscription.event.predicate)) {
                exploder.fireEvent(subscription.id, eventAttributes)
            }
        }
    }

    private fun evaluateAttributes(
        attributes: Map<String, DataValue>,
        model: Map<String, String>,
        additionalMeta: Map<String, Any>
    ): ObjectData {
        val result = mutableMapOf<String, DataValue>()

        RequestContext.doWithCtx(
            recordsServiceFactory,
            { data ->
                data.withCtxAtts(additionalMeta)
            }
        ) {
            recordsService.getAtts(attributes, model).forEach { key, attr ->
                result[key] = attr
            }
        }

        return ObjectData.create(result)
    }

    private fun ObjectData.isMatchPredicates(predicateData: String?): Boolean {
        if (predicateData.isNullOrBlank()) {
            return true
        }

        val predicate = Json.mapper.read(predicateData, Predicate::class.java)
        if (predicate == null || predicate is VoidPredicate) {
            return true
        }

        val element = RecordAttsElement.create(RecordAtts(RecordRef.EMPTY, this))
        val resolvedFilter = recordsTemplateService.resolve(
            predicate,
            RecordRef.create("meta", "")
        )

        return predicateService.isMatch(element, resolvedFilter)
    }
}

fun GeneralEvent.toIncomingEventData(): IncomingEventData {
    return IncomingEventData(
        eventName = this.type,
        record = EntityRef.valueOf(this.record)
    )
}
