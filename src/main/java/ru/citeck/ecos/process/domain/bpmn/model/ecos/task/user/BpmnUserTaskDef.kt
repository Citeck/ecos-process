package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.records2.RecordRef

data class BpmnUserTaskDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val outcomes: List<TaskOutcome> = emptyList(),
    val assignees: List<Recipient> = emptyList(),

    val manualRecipientsMode: Boolean = false,
    val manualRecipients: List<String> = emptyList(),

    val formRef: RecordRef,
    val priority: TaskPriority,

    val multiInstanceConfig: MultiInstanceConfig? = null,

    private var multiInstanceAutoMode_: Boolean = false
) {

    val multiInstanceAutoMode: Boolean get() = multiInstanceAutoMode_

    init {
        if (!manualRecipientsMode && multiInstanceConfig != null) {
            multiInstanceAutoMode_ = true
        }

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

        if (RecordRef.isEmpty(formRef)) {
            throw EcosBpmnElementDefinitionException(id, "Task form ref cannot be empty.")
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
    }
}
