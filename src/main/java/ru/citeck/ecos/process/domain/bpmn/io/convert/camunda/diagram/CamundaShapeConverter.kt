package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram

import ru.citeck.ecos.process.domain.bpmn.io.BPMN_BIOCOLOR_FILL
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_BIOCOLOR_STROKE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_COLOR_BACKGROUND_COLOR
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_COLOR_BORDER_COLOR
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toOmg
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnShapeDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.BPMNShape
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaShapeConverter : EcosOmgConverter<BpmnShapeDef, BPMNShape> {

    override fun import(element: BPMNShape, context: ImportContext): BpmnShapeDef {
        error("Not supported")
    }

    override fun export(element: BpmnShapeDef, context: ExportContext): BPMNShape {
        return BPMNShape().apply {
            id = element.id
            bpmnElement = QName("", element.elementRef)
            bounds = element.bounds.toOmg()
            isIsExpanded = element.expanded

            otherAttributes.putIfNotBlank(BPMN_COLOR_BACKGROUND_COLOR, element.colored?.backgroundColor)
            otherAttributes.putIfNotBlank(BPMN_COLOR_BORDER_COLOR, element.colored?.borderColor)
            otherAttributes.putIfNotBlank(BPMN_BIOCOLOR_STROKE, element.colored?.stroke)
            otherAttributes.putIfNotBlank(BPMN_BIOCOLOR_FILL, element.colored?.fill)
        }
    }
}
