package ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnStartEventDef(
    val id: String,
    val name: MLText,
    val number: String,
    val documentation: MLText,

    val outgoing: List<String>,

    val isInterrupting: Boolean = true,

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val eventDefinition: BpmnAbstractEventDef?
)
