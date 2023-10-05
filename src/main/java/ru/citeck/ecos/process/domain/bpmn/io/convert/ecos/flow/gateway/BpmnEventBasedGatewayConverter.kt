package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NUMBER
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnEventBasedGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TEventBasedGateway
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnEventBasedGatewayConverter : EcosOmgConverter<BpmnEventBasedGatewayDef, TEventBasedGateway> {

    override fun import(element: TEventBasedGateway, context: ImportContext): BpmnEventBasedGatewayDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnEventBasedGatewayDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER] ?: "",
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
        )
    }

    override fun export(element: BpmnEventBasedGatewayDef, context: ExportContext): TEventBasedGateway {
        return TEventBasedGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, element.number)

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }
        }
    }
}
