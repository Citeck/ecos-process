package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time

import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException

data class TimeValue(
    val type: TimeType,
    val value: String
) {
    init {
        if (value.isBlank()) throw EcosBpmnDefinitionException("Time value cannot be blank")
    }
}
