package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_VARIABLE_EVENTS
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_VARIABLE_NANE
import ru.citeck.ecos.process.domain.bpmn.io.convert.expressionToTExpression
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.scriptToTExpression
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnConditionalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TConditionalEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaConditionalEventDefinitionConverter :
    EcosOmgConverter<BpmnConditionalEventDef, TConditionalEventDefinition> {
    override fun import(element: TConditionalEventDefinition, context: ImportContext): BpmnConditionalEventDef {
        error("Not supported")
    }

    override fun export(element: BpmnConditionalEventDef, context: ExportContext): TConditionalEventDefinition {
        return TConditionalEventDefinition().apply {
            id = element.id

            otherAttributes.putIfNotBlank(CAMUNDA_VARIABLE_NANE, element.variableName)
            otherAttributes.putIfNotBlank(
                CAMUNDA_VARIABLE_EVENTS,
                element.variableEvents.joinToString(",") { it.name.lowercase() }
            )

            when (element.condition.type) {
                ConditionType.EXPRESSION -> condition = element.condition.config.expressionToTExpression()
                ConditionType.SCRIPT -> condition = element.condition.config.scriptToTExpression()
                else -> {}
            }
        }
    }
}
