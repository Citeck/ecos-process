package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnParallelGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParallelGateway
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnParallelGatewayConverter : EcosOmgConverter<BpmnParallelGatewayDef, TParallelGateway> {

    override fun import(element: TParallelGateway, context: ImportContext): BpmnParallelGatewayDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnParallelGatewayDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER] ?: "",
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig()
        )
    }

    override fun export(element: BpmnParallelGatewayDef, context: ExportContext): TParallelGateway {
        return TParallelGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, element.number)
            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))
        }
    }
}
