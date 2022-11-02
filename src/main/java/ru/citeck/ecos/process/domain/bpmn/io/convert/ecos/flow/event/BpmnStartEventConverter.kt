package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ASYNC_CONFIG
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_JOB_CONFIG
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.convertToBpmnEventDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.fillBpmnEventDefPayloadFromBpmnEventDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnStartEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TStartEvent
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnStartEventConverter : EcosOmgConverter<BpmnStartEventDef, TStartEvent> {

    override fun import(element: TStartEvent, context: ImportContext): BpmnStartEventDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnStartEventDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            isInterrupting = element.isIsInterrupting,
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            outgoing = element.outgoing.map { it.localPart },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            eventDefinition = element.convertToBpmnEventDef(context)
        )
    }

    override fun export(element: BpmnStartEventDef, context: ExportContext): TStartEvent {
        return TStartEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            isIsInterrupting = element.isInterrupting

            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))

            element.eventDefinition?.let { fillBpmnEventDefPayloadFromBpmnEventDef(it, context) }
        }
    }
}
