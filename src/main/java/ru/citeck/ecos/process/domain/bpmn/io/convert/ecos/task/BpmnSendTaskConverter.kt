package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnSendTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.namespace.QName

class BpmnSendTaskConverter : EcosOmgConverter<BpmnSendTaskDef, TSendTask> {

    override fun import(element: TSendTask, context: ImportContext): BpmnSendTaskDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnSendTaskDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            template = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_TEMPLATE]),
            record = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_RECORD]),
            title = element.otherAttributes[BPMN_PROP_NOTIFICATION_TITLE] ?: "",
            body = element.otherAttributes[BPMN_PROP_NOTIFICATION_BODY] ?: ""
        )
    }

    override fun export(element: BpmnSendTaskDef, context: ExportContext): TSendTask {
        return TSendTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TEMPLATE, element.template.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_RECORD, element.record.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TITLE, element.title)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_BODY, element.body)
        }
    }
}
