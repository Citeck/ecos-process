package ru.citeck.ecos.process.domain.cmmn.io.convert.artifact

import ru.citeck.ecos.process.domain.cmmn.model.omg.TTextAnnotation
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.TextAnnotationDef

class TextAnnotationConverter  : CmmnConverter<TTextAnnotation, TextAnnotationDef> {

    companion object {
        const val TYPE = "TextAnnotation"
    }

    override fun import(element: TTextAnnotation, context: ImportContext): TextAnnotationDef {

        return TextAnnotationDef(
            element.text ?: "",
            element.textFormat
        )
    }

    override fun export(element: TextAnnotationDef, context: ExportContext): TTextAnnotation {

        val tTextAnn = TTextAnnotation()

        tTextAnn.text = element.text
        tTextAnn.textFormat = element.textFormat

        return tTextAnn
    }

    override fun getElementType() = TYPE
}
