package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.io.Serializable
import java.time.Instant

data class ProcTaskDto(

    val id: String,

    val name: MLText = MLText.EMPTY,

    val priority: Int = 0,

    val isDeleted: Boolean = false,

    val possibleOutcomes: List<TaskOutcome> = emptyList(),

    val formRef: RecordRef = RecordRef.EMPTY,

    val documentRef: RecordRef = RecordRef.EMPTY,

    val processInstanceId: RecordRef = RecordRef.EMPTY,

    val created: Instant,

    val ended: Instant? = null,

    val durationInMillis: Long? = null,

    val dueDate: Instant? = null,

    val assignee: EntityRef = RecordRef.EMPTY,

    val candidateUsers: List<EntityRef> = emptyList(),

    val candidateGroups: List<EntityRef> = emptyList(),

    val definitionKey: String? = null,

    val historic: Boolean = false,

    val engineAtts: List<String> = emptyList()

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
