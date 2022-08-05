package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

data class ConditionConfig(
    val fn: String = "",
    val expression: String = "",
    val outcome: Outcome = Outcome.EMPTY
)
