package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script

data class JobConfig(
    val jobPriority: Long? = null,
    val jobRetryTimeCycle: String? = null,
)
