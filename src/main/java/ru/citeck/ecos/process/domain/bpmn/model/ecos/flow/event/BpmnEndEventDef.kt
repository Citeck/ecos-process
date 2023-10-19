package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnEndEventDef(
    val id: String,
    val name: MLText,
    val number: Int?,
    val documentation: MLText,

    val incoming: List<String>,

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val eventDefinition: BpmnAbstractEventDef?
)
