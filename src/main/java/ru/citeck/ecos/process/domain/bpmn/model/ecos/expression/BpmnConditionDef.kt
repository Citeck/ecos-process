package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

data class BpmnConditionDef(
    val type: ConditionType = ConditionType.NONE,
    val config: ConditionConfig = ConditionConfig()
)
