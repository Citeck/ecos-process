package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.convertToBpmnEventDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.fillBpmnEventDefPayloadFromBpmnEventDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnBoundaryEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBoundaryEvent
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnBoundaryEventConverter : EcosOmgConverter<BpmnBoundaryEventDef, TBoundaryEvent> {

    override fun import(element: TBoundaryEvent, context: ImportContext): BpmnBoundaryEventDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnBoundaryEventDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            attachedToRef = element.attachedToRef.localPart,
            cancelActivity = element.isCancelActivity,
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            eventDefinition = element.convertToBpmnEventDef(context)
        )
    }

    override fun export(element: BpmnBoundaryEventDef, context: ExportContext): TBoundaryEvent {
        return TBoundaryEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            attachedToRef = QName("", element.attachedToRef)
            isCancelActivity = element.cancelActivity

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
            element.eventDefinition?.let { fillBpmnEventDefPayloadFromBpmnEventDef(it, context) }
        }
    }
}
