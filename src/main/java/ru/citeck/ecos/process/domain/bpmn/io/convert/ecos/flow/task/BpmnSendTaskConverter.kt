package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.task

import org.apache.commons.lang3.LocaleUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsToJsonWithoutType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.BpmnSendTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.CalendarEventOrganizer
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnSendTaskConverter : EcosOmgConverter<BpmnSendTaskDef, TSendTask> {

    override fun import(element: TSendTask, context: ImportContext): BpmnSendTaskDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnSendTaskDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            type = NotificationType.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_TYPE]!!),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            template = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_TEMPLATE]),
            record = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_NOTIFICATION_RECORD]),
            title = element.otherAttributes[BPMN_PROP_NOTIFICATION_TITLE] ?: "",
            body = element.otherAttributes[BPMN_PROP_NOTIFICATION_BODY] ?: "",
            from = element.otherAttributes[BPMN_PROP_NOTIFICATION_FROM] ?: "",
            to = recipientsFromJson(
                mapOf(
                    RecipientType.ROLE to element.otherAttributes[BPMN_PROP_NOTIFICATION_TO],
                    RecipientType.EXPRESSION to element.otherAttributes[BPMN_PROP_NOTIFICATION_TO_EXPRESSION]
                )
            ),
            cc = recipientsFromJson(
                mapOf(
                    RecipientType.ROLE to element.otherAttributes[BPMN_PROP_NOTIFICATION_CC],
                    RecipientType.EXPRESSION to element.otherAttributes[BPMN_PROP_NOTIFICATION_CC_EXPRESSION]
                )
            ),
            bcc = recipientsFromJson(
                mapOf(
                    RecipientType.ROLE to element.otherAttributes[BPMN_PROP_NOTIFICATION_BCC],
                    RecipientType.EXPRESSION to element.otherAttributes[BPMN_PROP_NOTIFICATION_BCC_EXPRESSION]
                )
            ),
            lang = LocaleUtils.toLocale(element.otherAttributes[BPMN_PROP_NOTIFICATION_LANG]),
            additionalMeta = element.otherAttributes[BPMN_PROP_NOTIFICATION_ADDITIONAL_META]?.let {
                Json.mapper.readMap(it, String::class.java, String::class.java)
            } ?: emptyMap(),
            sendCalendarEvent = element.otherAttributes[BPMN_PROP_NOTIFICATION_SEND_CALENDAR_EVENT].toBoolean(),
            calendarEventSummary = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_SUMMARY] ?: "",
            calendarEventDescription = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DESCRIPTION] ?: "",
            calendarEventOrganizer = CalendarEventOrganizer(
                element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_ORGANIZER] ?: "",
                element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_ORGANIZER_EXPRESSION] ?: ""
            ),
            calendarEventDate = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DATE] ?: "",
            calendarEventDateExpression = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DATE_EXPRESSION] ?: "",
            calendarEventDuration = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DURATION] ?: "",
            calendarEventDurationExpression = element.otherAttributes[BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DURATION_EXPRESSION] ?: "",
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig()
        )
    }

    override fun export(element: BpmnSendTaskDef, context: ExportContext): TSendTask {
        return TSendTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TEMPLATE, element.template.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TYPE, element.type.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_RECORD, element.record.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_TITLE, element.title)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_BODY, element.body)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_FROM, element.from)

            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_TO,
                recipientsToJsonWithoutType(element.to.filter { it.type == RecipientType.ROLE })
            )
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_TO_EXPRESSION,
                recipientsToJsonWithoutType(element.to.filter { it.type == RecipientType.EXPRESSION })
            )

            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_CC,
                recipientsToJsonWithoutType(element.cc.filter { it.type == RecipientType.ROLE })
            )
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_CC_EXPRESSION,
                recipientsToJsonWithoutType(element.cc.filter { it.type == RecipientType.EXPRESSION })
            )

            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_BCC,
                recipientsToJsonWithoutType(element.bcc.filter { it.type == RecipientType.ROLE })
            )
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_BCC_EXPRESSION,
                recipientsToJsonWithoutType(element.bcc.filter { it.type == RecipientType.EXPRESSION })
            )

            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_LANG, element.lang.toString())
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_ADDITIONAL_META,
                Json.mapper.toString(element.additionalMeta)
            )

            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_SEND_CALENDAR_EVENT, element.sendCalendarEvent.toString())

            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_SUMMARY, element.calendarEventSummary)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DESCRIPTION, element.calendarEventDescription)

            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_ORGANIZER,
                element.calendarEventOrganizer.role
            )
            otherAttributes.putIfNotBlank(
                BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_ORGANIZER_EXPRESSION,
                element.calendarEventOrganizer.expression
            )

            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DATE, element.calendarEventDate)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DATE_EXPRESSION, element.calendarEventDateExpression)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DURATION, element.calendarEventDuration)
            otherAttributes.putIfNotBlank(BPMN_PROP_NOTIFICATION_CALENDAR_EVENT_DURATION_EXPRESSION, element.calendarEventDurationExpression)

            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
        }
    }
}
