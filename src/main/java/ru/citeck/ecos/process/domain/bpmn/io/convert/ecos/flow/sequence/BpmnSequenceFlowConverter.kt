package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.sequence

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence.BpmnSequenceFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnSequenceFlowConverter : EcosOmgConverter<BpmnSequenceFlowDef, TSequenceFlow> {

    override fun import(element: TSequenceFlow, context: ImportContext): BpmnSequenceFlowDef {
        return BpmnSequenceFlowDef(
            id = element.id,
            sourceRef = (element.sourceRef as TBaseElement).id,
            targetRef = (element.targetRef as TBaseElement).id
        )
    }

    override fun export(element: BpmnSequenceFlowDef, context: ExportContext): TSequenceFlow {
        return TSequenceFlow().apply {
            id = element.id
            sourceRef = element.sourceRef
            targetRef = element.targetRef
        }
    }
}
