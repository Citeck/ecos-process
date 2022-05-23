package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import java.util.*

data class ProcTaskDto(

    val id: String,

    val name: MLText = MLText.EMPTY,

    val formRef: RecordRef = RecordRef.EMPTY,

    val documentRef: RecordRef = RecordRef.EMPTY,

    val created: Date,

    val dueDate: Date? = null,

    val assignee: RecordRef =  RecordRef.EMPTY,

    val candidateUsers: List<RecordRef> = emptyList(),

    val candidateGroups: List<RecordRef> = emptyList(),

    val definitionKey: String? = null

)
