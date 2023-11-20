package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time

import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

data class TimeValue(
    val type: TimeType,
    val value: String
) : Validated {

    override fun validate() {
        if (value.isBlank()) {
            throw EcosBpmnDefinitionException("Time value cannot be blank")
        }
    }
}
