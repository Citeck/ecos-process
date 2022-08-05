package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.process.domain.bpmn.io.propMandatoryError
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnProcessConverter : EcosOmgConverter<BpmnProcessDef, TProcess> {

    override fun import(element: TProcess, context: ImportContext): BpmnProcessDef {
        return BpmnProcessDef(
            id = element.id,
            isExecutable = element.isIsExecutable,
            flowElements = element.flowElement.map { importElement(it.value, context) },
            artifacts = element.artifact.map { importArtifact(it.value, context) }
        )
    }

    private fun importElement(element: TFlowElement, context: ImportContext): BpmnFlowElementDef {
        val flowElement = context.converters.import(element, context)

        return BpmnFlowElementDef(
            id = element.id,
            type = flowElement.type,
            data = flowElement.data
        )
    }

    private fun importArtifact(element: TArtifact, context: ImportContext): BpmnArtifactDef {
        val artifactElement = context.converters.import(element, context)

        return BpmnArtifactDef(
            id = element.id,
            type = artifactElement.type,
            data = artifactElement.data
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

    private fun fillElementsRefsFromIdToRealObjects(tFlowElements: List<TBaseElement>, context: ExportContext) {
        tFlowElements.forEach { element ->
            if (element is TSequenceFlow) {
                element.sourceRef = context.bpmnElementsById[element.sourceRef.toString()]
                    ?: propMandatoryError("sourceRef", element::class)
                element.targetRef = context.bpmnElementsById[element.targetRef.toString()]
                    ?: propMandatoryError("targetRef", element::class)
            }

            if (element is TExclusiveGateway) {
                if (element.default != null) {
                    element.default = context.bpmnElementsById[element.default]
                        ?: propMandatoryError("default", element::class)
                }
            }
        }
    }
}
