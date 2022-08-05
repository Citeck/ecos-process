package ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async

data class AsyncConfig(
    val asyncBefore: Boolean = false,
    val asyncAfter: Boolean = false,
    val exclusive: Boolean = true
)
