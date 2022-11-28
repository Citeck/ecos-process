package ru.citeck.ecos.process.domain.bpmn.event

import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscriptionCombiner

fun Collection<String>.addDefaultEventAtts(): Collection<String> {
    return this + EventSubscriptionCombiner.DEFAULT_ATTS
}

fun Collection<String>.addDefaultAttsOfEventRepresentations(eventName: String): Collection<String> {
    return this + EcosEventType.from(eventName).eventRepresentations.map {
        it.defaultModel.values
    }.flatten()
}
