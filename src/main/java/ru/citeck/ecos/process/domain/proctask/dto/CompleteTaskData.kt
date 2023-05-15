package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_COMMENT
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome

data class CompleteTaskData(
    val task: ProcTaskDto,
    val outcome: Outcome,
    val variables: Map<String, Any?>
)

fun CompleteTaskData.getComment(): String? {
    val comment = variables[BPMN_COMMENT] as? String

    return if (comment.isNullOrBlank()) {
        null
    } else {
        comment
    }
}
