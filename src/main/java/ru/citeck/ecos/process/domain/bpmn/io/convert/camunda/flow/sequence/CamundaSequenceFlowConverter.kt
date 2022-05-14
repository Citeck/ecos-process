package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.sequence

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTExpression
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence.BpmnSequenceFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records3.record.request.RequestContext

// Currently, outcomes only supported
class CamundaSequenceFlowConverter : EcosOmgConverter<BpmnSequenceFlowDef, TSequenceFlow> {

    override fun import(element: TSequenceFlow, context: ImportContext): BpmnSequenceFlowDef {
        error("Not supported")
    }

    override fun export(element: BpmnSequenceFlowDef, context: ExportContext): TSequenceFlow {
        return TSequenceFlow().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            sourceRef = element.sourceRef
            targetRef = element.targetRef

            if (element.condition.type == ConditionType.OUTCOME && element.condition.config.outcome != Outcome.EMPTY) {
                conditionExpression = element.condition.config.outcome.toTExpression()
            }
        }
    }
}
