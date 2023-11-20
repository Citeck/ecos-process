package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnTaskDef(
    val id: String,
    val name: MLText,
    val number: Int?,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,
    val multiInstanceConfig: MultiInstanceConfig? = null,

    val ecosTaskDefinition: BpmnAbstractEcosTaskDef? = null
)
