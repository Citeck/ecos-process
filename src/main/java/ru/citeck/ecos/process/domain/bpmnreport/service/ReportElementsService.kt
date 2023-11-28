package ru.citeck.ecos.process.domain.bpmnreport.service

import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.model.lib.status.service.StatusService
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmnreport.model.*
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class ReportElementsService(
    private val statusService: StatusService,
    private val roleService: RoleService,
    private val recordsService: RecordsService
) {
    companion object {
        private val MANUAL_MODE_NAME = MLText(
            LocaleUtils.toLocale("en") to "Manual setting",
            LocaleUtils.toLocale("ru") to "Ручная настройка"
        )
    }

    fun convertReportStatusElement(flowElement: BpmnFlowElementDef, ecosType: EntityRef): ReportStatusElement {
        val statusElement = ReportStatusElement()
        statusElement.name = Json.mapper.convert(flowElement.data["name"], MLText::class.java)
        statusElement.status =
            statusService.getStatusDefByType(ecosType, flowElement.data["ecosTaskDefinition"]["status"].asText())?.name
        return statusElement
    }

    fun convertReportGatewayElement(flowElement: BpmnFlowElementDef, elementType: ElementType): ReportBaseElement {
        val gatewayElement = ReportBaseElement()
        updateBaseElement(gatewayElement, flowElement, elementType)
        return gatewayElement
    }

    fun convertReportSubProcessElement(
        flowElement: BpmnFlowElementDef,
        elementType: ElementType
    ): ReportSubProcessElement {
        val subProcessElement = ReportSubProcessElement()
        updateBaseElement(subProcessElement, flowElement, elementType)
        return subProcessElement
    }

    fun convertReportCallActivityElement(
        flowElement: BpmnFlowElementDef,
        elementType: ElementType
    ): ReportSubProcessElement {
        val subProcessElement = ReportSubProcessElement()
        updateBaseElement(subProcessElement, flowElement, elementType)

        subProcessElement.subProcessName = getNameByEntity(flowElement.data["processRef"].asText())
            .takeUnless { it.isBlank() } ?: flowElement.data["calledElement"].asText()

        return subProcessElement
    }

    fun convertReportEventElement(flowElement: BpmnFlowElementDef, elementType: ElementType): ReportEventElement {
        val eventElement = ReportEventElement()
        updateBaseElement(eventElement, flowElement, elementType)

        when (flowElement.data["eventDefinition"]["type"].asText()) {
            "signalEvent" -> {

                eventElement.type = "Signal ${eventElement.type}"
                val cancelActivity = flowElement.data.get("cancelActivity", Boolean::class.java)
                if (cancelActivity != null && !cancelActivity) {
                    eventElement.type = "${eventElement.type} (Non Interrupting)"
                }

                Json.mapper.convert(flowElement.data["eventDefinition"], BpmnSignalEventDef::class.java)?.let { event ->
                    if (event.eventManualMode) {
                        eventElement.eventType = MANUAL_MODE_NAME
                        eventElement.value = event.manualSignalName
                    } else {
                        eventElement.eventType = EventType.values().find { it.name == event.eventType?.name }?.nameEvent
                    }
                }
            }

            "timerEvent" -> {

                eventElement.type = "Timer ${eventElement.type}"
                val cancelActivity = flowElement.data.get("cancelActivity", Boolean::class.java)
                if (cancelActivity != null && !cancelActivity) {
                    eventElement.type = "${eventElement.type} (Non Interrupting)"
                }

                val event = Json.mapper.convert(flowElement.data["eventDefinition"], BpmnTimerEventDef::class.java)
                event?.let {
                    eventElement.eventType = EventType.values().find { it.name == event.value.type.name }?.nameEvent
                    eventElement.value = event.value.value
                }
            }

            "errorEvent" -> {
                eventElement.type = "Error ${eventElement.type}"
                val event = Json.mapper.convert(flowElement.data["eventDefinition"], BpmnErrorEventDef::class.java)
                event?.let {
                    eventElement.value = event.errorName
                }
            }
        }
        return eventElement
    }

    fun convertReportTaskElement(
        flowElement: BpmnFlowElementDef,
        elementType: ElementType,
        ecosType: EntityRef?
    ): ReportTaskElement {
        val taskElement = ReportTaskElement()
        updateBaseElement(taskElement, flowElement, elementType)

        fun convertReportSendTaskRecipientElement(
            field: String
        ) {
            val recipients = flowElement.data[field].asList(DataValue::class.java)
            if (recipients.isEmpty()) {
                return
            }

            val recipientElement = ReportSendTaskRecipientElement()

            recipients.forEach {
                when (it["type"].asText()) {
                    RecipientType.ROLE.name -> {
                        recipientElement.roles = recipientElement.roles ?: ArrayList()
                        recipientElement.roles?.add(
                            ReportRoleElement(roleService.getRoleDef(ecosType, it["value"].asText()).name)
                        )
                    }

                    RecipientType.EXPRESSION.name -> {
                        recipientElement.expressions = recipientElement.expressions ?: ArrayList()
                        recipientElement.expressions?.add(
                            it["value"].asText()
                        )
                    }
                }
            }

            taskElement.recipients = taskElement.recipients ?: ReportSendTaskRecipientsElement()
            when (field) {
                "to" -> taskElement.recipients!!.to = recipientElement
                "cc" -> taskElement.recipients!!.cc = recipientElement
                "bcc" -> taskElement.recipients!!.bcc = recipientElement
            }
        }

        when (elementType) {
            ElementType.USER_TASK -> {
                for (outcome in flowElement.data["outcomes"]) {
                    taskElement.outcomes = taskElement.outcomes ?: ArrayList()
                    taskElement.outcomes?.add(
                        ReportUserTaskOutcomeElement(
                            Json.mapper.convert(outcome["name"], MLText::class.java) ?: MLText.EMPTY
                        )
                    )
                }

                val assignees = flowElement.data["assignees"]
                for (assignee in assignees) {
                    taskElement.assignees = taskElement.assignees ?: ReportUserTaskAssigneeElement()
                    taskElement.assignees?.roles = taskElement.assignees?.roles ?: ArrayList()
                    taskElement.assignees?.roles?.add(
                        ReportRoleElement(
                            roleService.getRoleDef(ecosType, assignee["value"].asText()).name
                        )
                    )
                }

                val manualRecipients = flowElement.data["manualRecipients"].asList(String::class.java)
                if (manualRecipients.isNotEmpty()) {
                    taskElement.assignees = taskElement.assignees ?: ReportUserTaskAssigneeElement()
                    taskElement.assignees?.customAssignees = ArrayList(manualRecipients)
                }
            }

            ElementType.SEND_TASK -> {
                convertReportSendTaskRecipientElement("to")
                convertReportSendTaskRecipientElement("cc")
                convertReportSendTaskRecipientElement("bcc")
            }

            ElementType.BUSINESS_RULE_TASK -> {
                taskElement.decisionName = getNameByEntity(flowElement.data["decisionRef"].asText())
            }

            ElementType.SERVICE_TASK -> {
                taskElement.service = ReportServiceTaskDefElement()
                when (flowElement.data["type"].asText()) {
                    ServiceTaskType.EXTERNAL.name -> {
                        taskElement.service?.type = ServiceTaskType.EXTERNAL.nameServiceType
                        taskElement.service?.topic = flowElement.data["externalTaskTopic"].asText()
                    }

                    ServiceTaskType.EXPRESSION.name -> {
                        taskElement.service?.type = ServiceTaskType.EXPRESSION.nameServiceType
                        taskElement.service?.expression = flowElement.data["expression"].asText()
                    }
                }
            }

            else -> {}
        }

        return taskElement
    }

    private fun updateBaseElement(
        baseElement: ReportBaseElement,
        flowElement: BpmnFlowElementDef,
        elementType: ElementType
    ) {
        baseElement.type = elementType.type
        baseElement.name =
            Json.mapper.convert(flowElement.data["name"], MLText::class.java)?.takeIf { !MLText.isEmpty(it) }
        baseElement.documentation =
            Json.mapper.convert(flowElement.data["documentation"], MLText::class.java)?.takeIf { !MLText.isEmpty(it) }
    }

    private fun getNameByEntity(entityRefStr: String): String {
        return recordsService.getAtt(entityRefStr, "name").asText().takeIf { it.isNotEmpty() }
            ?: EntityRef.valueOf(entityRefStr).getLocalId()
    }
}

enum class EventType(val nameEvent: MLText) {
    DATE(
        MLText(
            LocaleUtils.toLocale("ru") to "Дата",
            LocaleUtils.toLocale("en") to "Date"
        )
    ),
    DURATION(
        MLText(
            LocaleUtils.toLocale("ru") to "Продолжительность",
            LocaleUtils.toLocale("en") to "Duration"
        )
    ),
    CYCLE(
        MLText(
            LocaleUtils.toLocale("ru") to "Цикл",
            LocaleUtils.toLocale("en") to "Cycle"
        )
    ),
    COMMENT_CREATE(
        MLText(
            LocaleUtils.toLocale("ru") to "Комментарий создан",
            LocaleUtils.toLocale("en") to "Comment create"
        )
    ),
    COMMENT_UPDATE(
        MLText(
            LocaleUtils.toLocale("ru") to "Комментарий обновлен",
            LocaleUtils.toLocale("en") to "Comment update"
        )
    ),
    COMMENT_DELETE(
        MLText(
            LocaleUtils.toLocale("ru") to "Комментарий удален",
            LocaleUtils.toLocale("en") to "Comment delete"
        )
    ),
    RECORD_STATUS_CHANGED(
        MLText(
            LocaleUtils.toLocale("ru") to "Статус изменен",
            LocaleUtils.toLocale("en") to "Status changed"
        )
    ),
    RECORD_CHANGED(
        MLText(
            LocaleUtils.toLocale("ru") to "Record обновлен",
            LocaleUtils.toLocale("en") to "Record changed"
        )
    ),
    RECORD_CREATED(
        MLText(
            LocaleUtils.toLocale("ru") to "Record создан",
            LocaleUtils.toLocale("en") to "Record created"
        )
    ),
    RECORD_DELETED(
        MLText(
            LocaleUtils.toLocale("ru") to "Record удален",
            LocaleUtils.toLocale("en") to "Record deleted"
        )
    )
}

enum class ServiceTaskType(val nameServiceType: MLText) {
    EXTERNAL(
        MLText(
            LocaleUtils.toLocale("en") to "External task",
            LocaleUtils.toLocale("ru") to "Внешняя задача"
        )
    ),
    EXPRESSION(
        MLText(
            LocaleUtils.toLocale("en") to "Expression",
            LocaleUtils.toLocale("ru") to "Выражение"
        )
    )
}
