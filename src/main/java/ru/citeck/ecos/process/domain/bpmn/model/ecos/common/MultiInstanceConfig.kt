package ru.citeck.ecos.process.domain.bpmn.model.ecos.common

import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class MultiInstanceConfig(
    var sequential: Boolean = false,
    val collection: String? = null,
    val element: String? = null,
    val loopCardinality: String? = null,
    val completionCondition: String? = null,

    val asyncConfig: AsyncConfig = AsyncConfig(),
    val jobConfig: JobConfig = JobConfig(),
)
