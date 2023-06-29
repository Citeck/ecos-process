package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ecos.com.fasterxml.jackson210.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.BpmnConditionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType

@JsonTypeName("conditionalEvent")
data class BpmnConditionalEventDef(
    override val id: String,

    override var elementId: String = "",

    val variableName: String = "",
    val variableEvents: Set<BpmnVariableEvents> = emptySet(),

    val condition: BpmnConditionDef
) : BpmnAbstractEventDef() {

    init {

        if (condition.type == ConditionType.OUTCOME) {
            throw EcosBpmnElementDefinitionException(
                id,
                "On conditional event, condition type cannot be OUTCOME."
            )
        }

        if (condition.type == ConditionType.NONE) {
            throw EcosBpmnElementDefinitionException(
                id,
                "On conditional event, condition type cannot be NONE."
            )
        }

    }

}
