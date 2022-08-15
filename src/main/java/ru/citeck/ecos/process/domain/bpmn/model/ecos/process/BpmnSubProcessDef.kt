package ru.citeck.ecos.process.domain.bpmn.model.ecos.process

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef

class BpmnSubProcessDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val triggeredByEvent: Boolean,
    val flowElements: List<BpmnFlowElementDef>,
    val artifacts: List<BpmnArtifactDef>,

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val multiInstanceConfig: MultiInstanceConfig? = null
)
