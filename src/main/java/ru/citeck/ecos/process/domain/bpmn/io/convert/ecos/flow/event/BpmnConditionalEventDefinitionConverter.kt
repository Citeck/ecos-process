package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOCUMENT_VARIABLES
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_REACT_ON_DOCUMENT_CHANGE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_VARIABLE_EVENTS
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_VARIABLE_NAME
import ru.citeck.ecos.process.domain.bpmn.io.convert.conditionFromAttributes
import ru.citeck.ecos.process.domain.bpmn.io.convert.expressionToTExpression
import ru.citeck.ecos.process.domain.bpmn.io.convert.scriptToTExpression
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnConditionalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnVariableEvents
import ru.citeck.ecos.process.domain.bpmn.model.omg.TConditionalEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnConditionalEventDefinitionConverter : EcosOmgConverter<BpmnConditionalEventDef, TConditionalEventDefinition> {
    override fun import(element: TConditionalEventDefinition, context: ImportContext): BpmnConditionalEventDef {
        val conditionalEvent = BpmnConditionalEventDef(
            id = element.id,
            reactOnDocumentChange = element.otherAttributes[BPMN_PROP_REACT_ON_DOCUMENT_CHANGE]?.toBoolean() ?: false,
            documentVariables = element.otherAttributes[BPMN_PROP_DOCUMENT_VARIABLES]?.let {
                Json.mapper.readList(it, String::class.java)
            } ?: emptyList(),
            variableName = element.otherAttributes[BPMN_PROP_VARIABLE_NAME] ?: "",
            variableEvents = element.otherAttributes[BPMN_PROP_VARIABLE_EVENTS]?.let {
                val type = Json.mapper.getSetType(BpmnVariableEvents::class.java)
                Json.mapper.convert(it, type)
            } ?: emptySet(),
            condition = conditionFromAttributes(element.otherAttributes)
        )

        context.conditionalEventDefs.add(conditionalEvent)

        return conditionalEvent
    }

    override fun export(element: BpmnConditionalEventDef, context: ExportContext): TConditionalEventDefinition {
        return TConditionalEventDefinition().apply {
            id = element.id

            when (element.condition.type) {
                ConditionType.EXPRESSION -> condition = element.condition.config.expressionToTExpression()
                ConditionType.SCRIPT -> condition = element.condition.config.scriptToTExpression()
                else -> {}
            }
        }
    }
}
