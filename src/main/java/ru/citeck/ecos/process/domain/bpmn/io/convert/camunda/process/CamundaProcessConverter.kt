package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.process

import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TArtifact
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLaneSet
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaProcessConverter : EcosOmgConverter<BpmnProcessDef, TProcess> {

    override fun import(element: TProcess, context: ImportContext): BpmnProcessDef {
        error("Not supported")
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

            val tLaneSet = element.lanes.map {
                val converted = context.converters.export<TLaneSet>(it, context)
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
            tLaneSet.forEach { laneSet.add(it) }
        }
    }
}
