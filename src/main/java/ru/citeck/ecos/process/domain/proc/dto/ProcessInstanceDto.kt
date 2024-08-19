package ru.citeck.ecos.process.domain.proc.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

data class ProcessInstanceDto(
    val id: UUID,
    val procType: String,
    val recordRef: EntityRef,
    val stateId: UUID,
    val created: Instant,
    val modified: Instant
)
