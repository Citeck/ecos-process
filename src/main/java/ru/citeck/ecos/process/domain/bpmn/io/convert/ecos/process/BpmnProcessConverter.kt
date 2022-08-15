package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.process

import ru.citeck.ecos.process.domain.bpmn.io.convert.toBpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.toBpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TArtifact
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnProcessConverter : EcosOmgConverter<BpmnProcessDef, TProcess> {

    override fun import(element: TProcess, context: ImportContext): BpmnProcessDef {
        return BpmnProcessDef(
            id = element.id,
            isExecutable = element.isIsExecutable,
            flowElements = element.flowElement.map { it.value.toBpmnFlowElementDef(context) },
            artifacts = element.artifact.map { it.value.toBpmnArtifactDef(context) }
        )
    }

    override fun export(element: BpmnProcessDef, context: ExportContext): TProcess {
        return TProcess().apply {
            id = element.id
            isIsExecutable = element.isExecutable

            val tFlowElements = element.flowElements.map {
                val converted = context.converters.export<TFlowElement>(it.type, it.data, context)
                context.bpmnElementsById[converted.id] = converted
                converted
            }

            val tArtifacts = element.artifacts.map {
                val converted = context.converters.export<TArtifact>(it.type, it.data, context)
                context.bpmnElementsById[converted.id] = converted
                converted
            }

            fillElementsRefsFromIdToRealObjects(tFlowElements, context)

            tFlowElements.forEach { flowElement.add(context.converters.convertToJaxb(it)) }
            tArtifacts.forEach { artifact.add(context.converters.convertToJaxb(it)) }
        }
    }

}
