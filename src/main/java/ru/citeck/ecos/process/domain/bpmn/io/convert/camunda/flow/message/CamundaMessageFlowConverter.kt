package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.message

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnMessageFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TMessageFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaMessageFlowConverter : EcosOmgConverter<BpmnMessageFlowDef, TMessageFlow> {
    override fun import(element: TMessageFlow, context: ImportContext): BpmnMessageFlowDef {
        error("Not supported")
    }

    override fun export(element: BpmnMessageFlowDef, context: ExportContext): TMessageFlow {
        return TMessageFlow().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            sourceRef = QName("", element.sourceRef)
            targetRef = QName("", element.targetRef)
        }
    }
}
