package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

@Component
class CamundaEventProcessor(
    private val exploder: CamundaEventExploder,
    private val eventSubscriptionFinder: CamundaEventSubscriptionFinder,
    private val predicateService: PredicateService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun processEvent(incomingEvent: GeneralEvent) {

        val time = measureTimeMillis {
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

                val attsFromPredicate = let {
                    val predicate = Json.mapper.read(subscription.event.predicate, Predicate::class.java)
                        ?: VoidPredicate.INSTANCE
                    return@let PredicateUtils.getAllPredicateAttributes(predicate).associateBy { it }
                }

                finalEventModel.putAll(attsFromPredicate)
                finalEventModel.putAll(subscription.event.model)

                val eventAttributes = evaluateAttributes(incomingEvent, finalEventModel)
                if (eventAttributes.isMatchPredicates(subscription.event.predicate)) {
                    log.debug {
                        "Event match predicates. Predicate: \n${subscription.event.predicate} \nAtts: \n$eventAttributes"
                    }
                    exploder.fireEvent(subscription.id, BpmnDataValue.create(eventAttributes))
                } else {
                    log.debug {
                        "Event doesn't match predicates. Predicate: \n${subscription.event.predicate} \nAtts: \n$eventAttributes"
                    }
                }
            }
        }

        log.trace { "Process event ${incomingEvent.id} in $time ms" }
    }

    private fun evaluateAttributes(
        incomingEvent: GeneralEvent,
        model: Map<String, String>
    ): ObjectData {
        val atts = incomingEvent.attributes
        val result = mutableMapOf<String, Any>()

        for ((attKey, attValueKey) in model) {
            if (atts.containsKey(attValueKey)) {
                // Skip event meta attributes. They are added on final result as object
                if (attKey.startsWith(EVENT_META_ATT)) {
                    continue
                }

                result[attKey] = atts[attValueKey]!!
            }
        }

        result[EVENT_META_ATT] = incomingEvent.toEventMeta()
        result[EcosEventType.RECORD_ATT] = getEntityRefByAttKey(incomingEvent.attributes, EcosEventType.RECORD_ATT)

        result[EcosEventType.RECORD_TYPE_ATT] =
            getEntityRefByAttKey(incomingEvent.attributes, EcosEventType.RECORD_TYPE_ATT)

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

        val recAtts = RecordAtts()
        recAtts.setAtts(this)
        val attsElement = RecordAttsEventElement(recAtts)

        val predicateTemplateAtts = TmplUtils.getAtts(predicate)
        val resolvedFilter = if (predicateTemplateAtts.isEmpty()) {
            predicate
        } else {
            val predicateElementAtts = attsElement.getAttributes(predicateTemplateAtts.toList())
            val predicateAtts = ObjectData.create()
            predicateTemplateAtts.forEach {
                predicateAtts[it] = predicateElementAtts.getAttribute(it)
            }
            TmplUtils.applyAtts(predicate, predicateAtts)
                ?: error("Apply atts can't be performed. Predicate: $predicate Attributes: $predicateAtts")
        }
        return predicateService.isMatch(attsElement, resolvedFilter)
    }
}

fun getEntityRefByAttKey(attributes: Map<String, DataValue>, key: String): EntityRef {
    val value = attributes[key] ?: return EntityRef.EMPTY
    return EntityRef.valueOf(value.asText())
}

fun GeneralEvent.toIncomingEventData(): IncomingEventData {
    return IncomingEventData(
        eventName = this.type,
        record = getEntityRefByAttKey(attributes, EcosEventType.RECORD_ATT),
        recordType = getEntityRefByAttKey(attributes, EcosEventType.RECORD_TYPE_ATT)
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

private class RecordAttsEventElement(val atts: RecordAtts) : Element {

    override fun getAttributes(attributes: List<String>): ElementAttributes {
        return RecordAttsEventElementAtts(atts)
    }
}

private class RecordAttsEventElementAtts(private val atts: RecordAtts) : ElementAttributes {

    override fun getAttribute(name: String): Any {
        if (name.startsWith("_meta.")) {
            return atts["_meta"][name.substringAfter("_meta.")]
        }
        return atts.getAtt(name)
    }
}
