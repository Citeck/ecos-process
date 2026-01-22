package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.BpmnConditionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

@JsonTypeName("conditionalEvent")
data class BpmnConditionalEventDef(
    override val id: String,

    override var elementId: String = "",

    val reactOnDocumentChange: Boolean = false,
    val documentVariables: List<String> = emptyList(),

    val variableName: String = "",
    val variableEvents: Set<BpmnVariableEvents> = emptySet(),

    val condition: BpmnConditionDef
) : BpmnAbstractEventDef(),
    Validated {

    override fun validate() {
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

        if (reactOnDocumentChange) {
            if (variableName.isNotBlank()) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "On conditional event, variable name cannot be set when react on document change."
                )
            }

            if (variableEvents.isNotEmpty()) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "On conditional event, variable events cannot be set when react on document change."
                )
            }
        }
    }
}
