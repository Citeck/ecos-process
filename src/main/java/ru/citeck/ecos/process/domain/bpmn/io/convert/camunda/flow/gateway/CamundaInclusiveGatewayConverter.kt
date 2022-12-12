package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnInclusiveGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TInclusiveGateway
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaInclusiveGatewayConverter : EcosOmgConverter<BpmnInclusiveGatewayDef, TInclusiveGateway> {

    override fun import(element: TInclusiveGateway, context: ImportContext): BpmnInclusiveGatewayDef {
        error("Not supported")
    }

    override fun export(element: BpmnInclusiveGatewayDef, context: ExportContext): TInclusiveGateway {
        return TInclusiveGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            default = element.default
        }
    }
}
