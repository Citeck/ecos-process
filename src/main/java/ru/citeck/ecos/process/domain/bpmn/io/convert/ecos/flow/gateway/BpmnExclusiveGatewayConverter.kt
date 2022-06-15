package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnExclusiveGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExclusiveGateway
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnExclusiveGatewayConverter : EcosOmgConverter<BpmnExclusiveGatewayDef, TExclusiveGateway> {

    override fun import(element: TExclusiveGateway, context: ImportContext): BpmnExclusiveGatewayDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name
        val default = let {
            if (element.default == null) {
                return@let null
            }

            if (element.default is TSequenceFlow) {
                return@let (element.default as TSequenceFlow).id
            }

            error("Default element $element type is not supported. Implementation required.")
        }

        return BpmnExclusiveGatewayDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            default = default
        )
    }

    override fun export(element: BpmnExclusiveGatewayDef, context: ExportContext): TExclusiveGateway {
        return TExclusiveGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            default = element.default
        }
    }
}
