package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task

import org.apache.commons.lang3.LocaleUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsToJsonWithoutType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnSendTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
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
            type = NotificationType.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_TYPE]!!),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            template = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_TEMPLATE]),
            record = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_RECORD]),
            title = element.otherAttributes[BPMN_PROP_NOTIFICATION_TITLE] ?: "",
            body = element.otherAttributes[BPMN_PROP_NOTIFICATION_BODY] ?: "",
            to = recipientsFromJson(RecipientType.ROLE, element.otherAttributes[BPMN_PROP_NOTIFICATION_TO] ?: ""),
            cc = recipientsFromJson(RecipientType.ROLE, element.otherAttributes[BPMN_PROP_NOTIFICATION_CC] ?: ""),
            bcc = recipientsFromJson(RecipientType.ROLE, element.otherAttributes[BPMN_PROP_NOTIFICATION_BCC] ?: ""),
            lang = LocaleUtils.toLocale(element.otherAttributes[BPMN_PROP_NOTIFICATION_LANG]),
            additionalMeta = element.otherAttributes[BPMN_PROP_NOTIFICATION_ADDITIONAL_META]?.let {
                Json.mapper.readMap(it, String::class.java, String::class.java)
            } ?: emptyMap()
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
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TYPE, element.type.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_RECORD, element.record.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TITLE, element.title)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_BODY, element.body)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TO, recipientsToJsonWithoutType(element.to))
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CC, recipientsToJsonWithoutType(element.cc))
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_BCC, recipientsToJsonWithoutType(element.bcc))
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_LANG, element.lang.toString())
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_ADDITIONAL_META,
                Json.mapper.toString(element.additionalMeta)
            )
        }
    }
}
