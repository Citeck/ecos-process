package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.sequence

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence.BpmnSequenceFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaSequenceFlowConverter : EcosOmgConverter<BpmnSequenceFlowDef, TSequenceFlow> {

    override fun import(element: TSequenceFlow, context: ImportContext): BpmnSequenceFlowDef {
        error("Not supported")
    }

    override fun export(element: BpmnSequenceFlowDef, context: ExportContext): TSequenceFlow {
       return TSequenceFlow().apply {
           id = element.id
           sourceRef = element.sourceRef
           targetRef = element.targetRef
       }
    }
}
