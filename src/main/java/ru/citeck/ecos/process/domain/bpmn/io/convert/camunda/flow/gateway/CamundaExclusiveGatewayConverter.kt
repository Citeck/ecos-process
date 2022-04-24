package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnExclusiveGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExclusiveGateway
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.namespace.QName

class CamundaExclusiveGatewayConverter : EcosOmgConverter<BpmnExclusiveGatewayDef, TExclusiveGateway> {

    override fun import(element: TExclusiveGateway, context: ImportContext): BpmnExclusiveGatewayDef {
        error("Not supported")
    }

    override fun export(element: BpmnExclusiveGatewayDef, context: ExportContext): TExclusiveGateway {
        return TExclusiveGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
        }
    }
}
