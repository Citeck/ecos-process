package ru.citeck.ecos.process.domain.bpmn.model.ecos

import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef

class BpmnProcessDef(
    val id: String,
    val isExecutable: Boolean,
    val flowElements: List<BpmnFlowElementDef>,
    val artifacts: List<BpmnArtifactDef>
)
