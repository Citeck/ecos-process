package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.meta.RecordsTemplateService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

@Component
class CamundaEventProcessor(
    private val exploder: CamundaEventExploder,
    private val eventSubscriptionFinder: CamundaEventSubscriptionFinder,
    private val predicateService: PredicateService,
    private val recordsTemplateService: RecordsTemplateService
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

        for (subscription in foundSubscriptions) {
            val finalEventModel = mutableMapOf<String, String>()
            finalEventModel.putAll(defaultModelForIncomingEvent)
            finalEventModel.putAll(subscription.event.model)

            val eventAttributes = evaluateAttributes(incomingEvent, finalEventModel)
            if (eventAttributes.isMatchPredicates(subscription.event.predicate)) {
                exploder.fireEvent(subscription.id, eventAttributes)
            }
        }
    }

    private fun evaluateAttributes(
        incomingEvent: GeneralEvent,
        model: Map<String, String>
    ): ObjectData {
        val atts = incomingEvent.attributes
        val result = mutableMapOf<String, DataValue>()

        for ((attKey, attValueKey) in model) {
            if (atts.containsKey(attValueKey)) {
                result[attKey] = atts[attValueKey]!!
            }
        }

        result[EVENT_META_ATT] = incomingEvent.toEventMeta()

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
        eventName = this.type
    )
}

fun GeneralEvent.toEventMeta(): DataValue {
    return DataValue.create(
        mapOf(
            EVENT_META_ID_ATT to this.id,
            EVENT_META_TYPE_ATT to this.type,
            EVENT_META_TIME_ATT to this.time,
            EVENT_META_USER_ATT to this.user
        )
    )
}
