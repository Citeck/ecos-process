package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
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

    val formRef: RecordRef,
    val priority: TaskPriority
) {

    init {
        if (assignees.isEmpty()) {
            throw EcosBpmnDefinitionException("Task assignees cannot be empty")
        }

        if (RecordRef.isEmpty(formRef)) {
            throw EcosBpmnDefinitionException("Task form ref cannot be empty")
        }

        if (outcomes.isEmpty()) {
            throw EcosBpmnDefinitionException("Task outcomes cannot be empty")
        }
    }

}
