package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.artifact

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnTextAnnotationDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTextAnnotation
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnTextAnnotationConverter : EcosOmgConverter<BpmnTextAnnotationDef, TTextAnnotation> {

    override fun import(element: TTextAnnotation, context: ImportContext): BpmnTextAnnotationDef {
        val text = element.otherAttributes[BPMN_PROP_NAME_ML] ?: ""

        return BpmnTextAnnotationDef(
            id = element.id,
            text = Json.mapper.convert(text, MLText::class.java) ?: MLText(),
        )
    }

    override fun export(element: BpmnTextAnnotationDef, context: ExportContext): TTextAnnotation {
        return TTextAnnotation().apply {
            id = element.id
            text = element.toTText()

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.text)
        }
    }
}
