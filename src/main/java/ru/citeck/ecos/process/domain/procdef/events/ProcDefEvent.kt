package ru.citeck.ecos.process.domain.procdef.events

import ru.citeck.ecos.records2.RecordRef

data class ProcDefEvent(
    val procDefRef: RecordRef,
    val version: Double,
    val createdFromVersion: Double = 0.0
)
