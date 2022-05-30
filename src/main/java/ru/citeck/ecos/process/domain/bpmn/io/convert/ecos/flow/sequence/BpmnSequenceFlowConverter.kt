package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.sequence

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_CONDITION_CONFIG
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_CONDITION_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.conditionFromAttributes
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence.BpmnSequenceFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnSequenceFlowConverter : EcosOmgConverter<BpmnSequenceFlowDef, TSequenceFlow> {

    override fun import(element: TSequenceFlow, context: ImportContext): BpmnSequenceFlowDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnSequenceFlowDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            sourceRef = (element.sourceRef as TBaseElement).id,
            targetRef = (element.targetRef as TBaseElement).id,
            condition = conditionFromAttributes(element.otherAttributes)
        )
    }

    override fun export(element: BpmnSequenceFlowDef, context: ExportContext): TSequenceFlow {
        return TSequenceFlow().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            sourceRef = element.sourceRef
            targetRef = element.targetRef

            otherAttributes[BPMN_PROP_CONDITION_TYPE] = element.condition.type.name
            otherAttributes[BPMN_PROP_CONDITION_CONFIG] = Json.mapper.toString(element.condition.config)
        }
    }
}
