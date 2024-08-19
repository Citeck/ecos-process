package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        val nameMl = let {
            val name = Json.mapper.convert(element.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java)
                ?: MLText.EMPTY
            return@let if (name == MLText.EMPTY) {
                MLText(element.name ?: "")
            } else {
                name
            }
        }

        return BpmnUserTaskDef(
            id = element.id,
            name = nameMl,
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            outcomes = Json.mapper.readList(element.otherAttributes[BPMN_PROP_OUTCOMES], TaskOutcome::class.java),
            manualRecipientsMode = element.otherAttributes[BPMN_PROP_MANUAL_RECIPIENTS_MODE]?.toBoolean() ?: false,
            manualRecipients = Json.mapper.readList(
                element.otherAttributes[BPMN_PROP_MANUAL_RECIPIENTS],
                String::class.java
            ),
            assignees = recipientsFromJson(
                mapOf(
                    RecipientType.ROLE to element.otherAttributes[BPMN_PROP_ASSIGNEES]
                )
            ),
            formRef = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_FORM_REF]),
            dueDate = element.otherAttributes[BPMN_PROP_DUE_DATE],
            followUpDate = element.otherAttributes[BPMN_PROP_FOLLOW_UP_DATE],
            priority = if (element.otherAttributes[BPMN_PROP_PRIORITY].isNullOrBlank()) {
                TaskPriority.MEDIUM
            } else {
                TaskPriority.valueOf(element.otherAttributes[BPMN_PROP_PRIORITY]!!)
            },
            priorityExpression = element.otherAttributes[BPMN_PROP_PRIORITY_EXPRESSION],
            multiInstanceConfig = element.toMultiInstanceConfig(),
            laEnabled = element.otherAttributes[BPMN_PROP_LA_ENABLED].toBoolean(),
            laNotificationType = element.otherAttributes[BPMN_PROP_LA_NOTIFICATION_TYPE]?.let {
                NotificationType.valueOf(it)
            },
            laNotificationTemplate = element.otherAttributes[BPMN_PROP_LA_NOTIFICATION_TEMPLATE]?.let {
                EntityRef.valueOf(it)
            },
            laManualNotificationTemplateEnabled =
            element.otherAttributes[BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE_ENABLED].toBoolean(),
            laManualNotificationTemplate = element.otherAttributes[BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE],
            laReportEnabled = element.otherAttributes[BPMN_PROP_LA_REPORT_ENABLED].toBoolean(),
            laSuccessReportNotificationTemplate =
            element.otherAttributes[BPMN_PROP_LA_SUCCESS_REPORT_NOTIFICATION_TEMPLATE]?.let {
                EntityRef.valueOf(it)
            },
            laErrorReportNotificationTemplate =
            element.otherAttributes[BPMN_PROP_LA_ERROR_REPORT_NOTIFICATION_TEMPLATE]?.let {
                EntityRef.valueOf(it)
            }

        )
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
            otherAttributes.putIfNotBlank(BPMN_PROP_ASSIGNEES, recipientsToJsonWithoutType(element.assignees))
            otherAttributes.putIfNotBlank(BPMN_PROP_FORM_REF, element.formRef.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_PRIORITY, element.priority.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_PRIORITY_EXPRESSION, element.priorityExpression)
            otherAttributes.putIfNotBlank(BPMN_PROP_DUE_DATE, element.dueDate)
            otherAttributes.putIfNotBlank(BPMN_PROP_FOLLOW_UP_DATE, element.followUpDate)

            otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_RECIPIENTS_MODE, element.manualRecipientsMode.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_RECIPIENTS, Json.mapper.toString(element.manualRecipients))

            otherAttributes.putIfNotBlank(BPMN_PROP_LA_ENABLED, element.laEnabled.toString())
            element.laNotificationType?.let {
                otherAttributes.putIfNotBlank(BPMN_PROP_LA_NOTIFICATION_TYPE, it.toString())
            }
            element.laNotificationTemplate?.let {
                otherAttributes.putIfNotBlank(BPMN_PROP_LA_NOTIFICATION_TEMPLATE, it.toString())
            }
            otherAttributes.putIfNotBlank(
                BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE_ENABLED,
                element.laManualNotificationTemplateEnabled.toString()
            )
            element.laManualNotificationTemplate?.let {
                otherAttributes.putIfNotBlank(BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE, it)
            }
            otherAttributes.putIfNotBlank(BPMN_PROP_LA_REPORT_ENABLED, element.laReportEnabled.toString())
            element.laSuccessReportNotificationTemplate?.let {
                otherAttributes.putIfNotBlank(BPMN_PROP_LA_SUCCESS_REPORT_NOTIFICATION_TEMPLATE, it.toString())
            }
            element.laErrorReportNotificationTemplate?.let {
                otherAttributes.putIfNotBlank(BPMN_PROP_LA_ERROR_REPORT_NOTIFICATION_TEMPLATE, it.toString())
            }

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
                otherAttributes[BPMN_MULTI_INSTANCE_CONFIG] = Json.mapper.toString(it)
            }
        }
    }
}
