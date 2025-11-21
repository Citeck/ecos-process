package ru.citeck.ecos.process.domain.procdef.events

import ru.citeck.ecos.webapp.api.entity.EntityRef

data class ProcDefEvent(
    val procDefRef: EntityRef,
    val workspace: String,
    val version: Double,
    val createdFromVersion: Double = 0.0,
    val dataState: String? = null
)
