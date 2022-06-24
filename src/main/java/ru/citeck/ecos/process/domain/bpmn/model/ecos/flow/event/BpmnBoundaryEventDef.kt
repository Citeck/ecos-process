package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnBoundaryEventDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val attachedToRef: String,
    val cancelActivity: Boolean = true,

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val eventDefinition: BpmnAbstractEventDef?
)
