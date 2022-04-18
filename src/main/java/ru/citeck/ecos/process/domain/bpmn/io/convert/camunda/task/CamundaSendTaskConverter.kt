package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_NS
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.SendNotificationDelegate
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.CamundaFieldCreator
import ru.citeck.ecos.process.domain.bpmn.io.convert.addIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.jaxb
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsToJson
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnSendTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class CamundaSendTaskConverter : EcosOmgConverter<BpmnSendTaskDef, TSendTask> {

    private val camundaClass = QName(CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CLASS)

    override fun import(element: TSendTask, context: ImportContext): BpmnSendTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnSendTaskDef, context: ExportContext): TSendTask {
        return TSendTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[camundaClass] = SendNotificationDelegate::class.java.name

            extensionElements = TExtensionElements()
            val notificationFields = getNotificationFields(element, context)
            extensionElements.any.addAll(notificationFields)
        }
    }

    private fun getNotificationFields(
        element: BpmnSendTaskDef,
        context: ExportContext
    ): List<JAXBElement<CamundaField>> {
        val fields = mutableListOf<CamundaField>()

        with(element) {
            fields.addIfNotBlank(
                CamundaFieldCreator.string(
                    BPMN_PROP_NOTIFICATION_TEMPLATE.localPart, template.toString()
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.string(
                    BPMN_PROP_NOTIFICATION_RECORD.localPart,
                    record.toString()
                )
            )
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_TYPE.localPart, type.toString()))
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_TITLE.localPart, title))
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_BODY.localPart, body))

            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_TO.localPart, recipientsToJson(to)))
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_CC.localPart, recipientsToJson(cc)))
            fields.addIfNotBlank(
                CamundaFieldCreator.string(
                    BPMN_PROP_NOTIFICATION_BCC.localPart,
                    recipientsToJson(bcc)
                )
            )

            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_LANG.localPart, lang.toString()))
            fields.addIfNotBlank(
                CamundaFieldCreator.string(
                    BPMN_PROP_NOTIFICATION_ADDITIONAL_META.localPart,
                    Json.mapper.toString(additionalMeta) ?: ""
                )
            )
        }

        return fields.map { it.jaxb(context) }
    }
}


