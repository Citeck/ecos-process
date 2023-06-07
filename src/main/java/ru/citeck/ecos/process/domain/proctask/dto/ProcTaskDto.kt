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

    val documentType: String? = null,

    val processInstanceId: RecordRef = RecordRef.EMPTY,

    val created: Instant,

    val ended: Instant? = null,

    val durationInMillis: Long? = null,

    val dueDate: Instant? = null,

    val followUpDate: Instant? = null,

    val assignee: EntityRef = RecordRef.EMPTY,

    val sender: EntityRef = RecordRef.EMPTY,

    val owner: EntityRef = RecordRef.EMPTY,

    val candidateUsers: List<EntityRef> = emptyList(),
    val candidateUsersOriginal: List<String> = emptyList(),

    val candidateGroups: List<EntityRef> = emptyList(),
    val candidateGroupsOriginal: List<String> = emptyList(),

    val definitionKey: String? = null,

    val historic: Boolean = false,

    val engineAtts: List<String> = emptyList(),

    val comment: String? = null,
    val lastComment: String? = null

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
