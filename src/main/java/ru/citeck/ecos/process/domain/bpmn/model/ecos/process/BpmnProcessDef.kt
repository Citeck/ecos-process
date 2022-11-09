package ru.citeck.ecos.process.domain.bpmn.model.ecos.process

import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneSetDef

class BpmnProcessDef(
    val id: String,
    val isExecutable: Boolean,
    val flowElements: List<BpmnFlowElementDef>,
    val lanes: List<BpmnLaneSetDef>,
    val artifacts: List<BpmnArtifactDef>
)
