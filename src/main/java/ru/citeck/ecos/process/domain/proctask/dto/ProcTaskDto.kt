package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

data class ProcTaskDto(

    val id: String,

    val name: MLText = MLText.EMPTY,

    val priority: Int = 0,

    val formRef: RecordRef = RecordRef.EMPTY,

    val documentRef: RecordRef = RecordRef.EMPTY,

    val processInstanceId: RecordRef = RecordRef.EMPTY,

    val created: Instant,

    val dueDate: Instant? = null,

    val assignee: EntityRef = RecordRef.EMPTY,

    val candidateUsers: List<EntityRef> = emptyList(),

    val candidateGroups: List<EntityRef> = emptyList(),

    val definitionKey: String? = null,

    val variables: Map<String, Any> = emptyMap()

)
