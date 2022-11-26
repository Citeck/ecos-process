package ru.citeck.ecos.process.domain.bpmn.event

import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscriptionCombiner

fun Collection<String>.addDefaultEventAtts(): Collection<String> {
    return this + EventSubscriptionCombiner.DEFAULT_ATTS
}
