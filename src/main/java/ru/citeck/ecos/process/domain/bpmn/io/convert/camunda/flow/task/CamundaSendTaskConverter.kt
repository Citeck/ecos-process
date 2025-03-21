package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send.SendNotificationDelegate
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnSendTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaSendTaskConverter : EcosOmgConverter<BpmnSendTaskDef, TSendTask> {

    override fun import(element: TSendTask, context: ImportContext): BpmnSendTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnSendTaskDef, context: ExportContext): TSendTask {
        return TSendTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[CAMUNDA_CLASS] = SendNotificationDelegate::class.java.name

            otherAttributes[CAMUNDA_ASYNC_BEFORE] = element.asyncConfig.asyncBefore.toString()
            otherAttributes[CAMUNDA_ASYNC_AFTER] = element.asyncConfig.asyncAfter.toString()
            otherAttributes[CAMUNDA_EXCLUSIVE] = element.asyncConfig.exclusive.toString()

            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            extensionElements = TExtensionElements().apply {
                any.addAll(getCamundaJobRetryTimeCycleFieldConfig(element.jobConfig.jobRetryTimeCycle, context))
            }

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
                    BPMN_PROP_NOTIFICATION_TEMPLATE.localPart,
                    template.toString()
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_RECORD.localPart,
                    record
                )
            )
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_TYPE.localPart, type.toString()))
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_TITLE.localPart, title))
            fields.addIfNotBlank(CamundaFieldCreator.string(BPMN_PROP_NOTIFICATION_BODY.localPart, body))

            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_FROM.localPart,
                    from
                )
            )

            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_TO.localPart,
                    recipientsToJson(to)
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CC.localPart,
                    recipientsToJson(cc)
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
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

            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_SEND_CALENDAR_EVENT.localPart,
                    sendCalendarEvent.toString()
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_SUMMARY.localPart,
                    calendarEventSummary
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DESCRIPTION.localPart,
                    calendarEventDescription
                )
            )
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_ORGANIZER.localPart,
                    Json.mapper.toString(calendarEventOrganizer) ?: ""
                )
            )

            val calendarEventDateField = if (StringUtils.isNotBlank(calendarEventDate)) {
                calendarEventDate
            } else {
                calendarEventDateExpression
            }
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DATE.localPart,
                    calendarEventDateField
                )
            )

            val calendarEventDurationField = if (StringUtils.isNotBlank(calendarEventDuration)) {
                calendarEventDuration
            } else {
                calendarEventDurationExpression
            }
            fields.addIfNotBlank(
                CamundaFieldCreator.expression(
                    BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DURATION.localPart,
                    calendarEventDurationField
                )
            )
        }

        return fields.map { it.jaxb(context) }
    }
}
