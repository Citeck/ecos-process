package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated
import ru.citeck.ecos.records2.RecordRef

data class BpmnUserTaskDef(
    val id: String,
    val name: MLText,
    val number: Int?,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val outcomes: List<TaskOutcome> = emptyList(),
    val assignees: List<Recipient> = emptyList(),

    val manualRecipientsMode: Boolean = false,
    val manualRecipients: List<String> = emptyList(),

    val formRef: RecordRef,
    val priority: TaskPriority,
    val priorityExpression: String? = null,

    val dueDate: String? = null,
    val followUpDate: String? = null,

    val multiInstanceConfig: MultiInstanceConfig? = null,

    private var multiInstanceAutoMode_: Boolean = false,

    val laEnabled: Boolean = false,
    val laNotificationType: NotificationType? = null,
    val laNotificationTemplate: RecordRef? = null,
    val laManualNotificationTemplateEnabled: Boolean = false,
    val laManualNotificationTemplate: String? = null,
    val laReportEnabled: Boolean = false,
    val laSuccessReportNotificationTemplate: RecordRef? = null,
    val laErrorReportNotificationTemplate: RecordRef? = null

) : Validated {

    val multiInstanceAutoMode: Boolean get() = multiInstanceAutoMode_

    init {
        if (!manualRecipientsMode && multiInstanceConfig != null) {
            multiInstanceAutoMode_ = true
        }
    }

    override fun validate() {
        if (manualRecipientsMode) {
            if (manualRecipients.isEmpty()) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "Manual recipients mode is enabled, but no manual recipients are specified."
                )
            }
        } else {
            if (assignees.isEmpty()) {
                throw EcosBpmnElementDefinitionException(id, "No assignees are specified at Task.")
            }
        }

        if (outcomes.isEmpty()) {
            throw EcosBpmnElementDefinitionException(id, "Task outcomes cannot be empty.")
        }

        if (multiInstanceAutoMode && manualRecipientsMode) {
            throw EcosBpmnElementDefinitionException(
                id,
                "Task can't be in multi-instance auto mode and manual recipients mode at the same time"
            )
        }

        if (laEnabled) {
            if (laNotificationType == null) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "Lazy approval notification type cannot be empty if lazy approval is enabled."
                )
            }

            if (laNotificationType == NotificationType.EMAIL_NOTIFICATION &&
                (laNotificationTemplate == null && laManualNotificationTemplate.isNullOrEmpty())
            ) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "Lazy approval with email notification type must have a template."
                )
            }
        }
    }
}
