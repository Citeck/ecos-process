package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.artifact

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnTextAnnotationDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTextAnnotation
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaTextAnnotationConverter : EcosOmgConverter<BpmnTextAnnotationDef, TTextAnnotation> {

    override fun import(element: TTextAnnotation, context: ImportContext): BpmnTextAnnotationDef {
        error("Not supported")
    }

    override fun export(element: BpmnTextAnnotationDef, context: ExportContext): TTextAnnotation {
        return TTextAnnotation().apply {
            id = element.id
            text = element.toTText()

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.text)
        }
    }
}
