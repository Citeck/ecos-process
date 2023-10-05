package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnParallelGatewayDef(
    val id: String,
    val name: MLText,
    val number: String,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,
)
