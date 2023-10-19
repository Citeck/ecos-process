package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NUMBER
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnInclusiveGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TInclusiveGateway
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnInclusiveGatewayConverter : EcosOmgConverter<BpmnInclusiveGatewayDef, TInclusiveGateway> {

    override fun import(element: TInclusiveGateway, context: ImportContext): BpmnInclusiveGatewayDef {
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

        return BpmnInclusiveGatewayDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            default = default
        )
    }

    override fun export(element: BpmnInclusiveGatewayDef, context: ExportContext): TInclusiveGateway {
        return TInclusiveGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
            default = element.default
        }
    }
}
