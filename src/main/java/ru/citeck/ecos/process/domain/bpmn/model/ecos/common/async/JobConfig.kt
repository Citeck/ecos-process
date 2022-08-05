package ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async

data class JobConfig(
    val jobPriority: Long? = null,
    val jobRetryTimeCycle: String? = null,
)
