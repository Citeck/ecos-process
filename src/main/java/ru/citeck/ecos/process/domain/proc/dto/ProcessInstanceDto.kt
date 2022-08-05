package ru.citeck.ecos.process.domain.proc.dto

import ru.citeck.ecos.records2.RecordRef
import java.time.Instant
import java.util.*

data class ProcessInstanceDto(
    val id: UUID,
    val procType: String,
    val recordRef: RecordRef,
    val stateId: UUID,
    val created: Instant,
    val modified: Instant
)
