package ru.citeck.ecos.process.domain.ecmmn.io.convert.artifact

import ru.citeck.ecos.process.domain.cmmn.model.TTextAnnotation
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.artifact.type.CmmnTextAnnotation

class TextAnnotationConverter  : CmmnConverter<TTextAnnotation, CmmnTextAnnotation> {

    companion object {
        const val TYPE = "TextAnnotation"
    }

    override fun import(element: TTextAnnotation, context: ImportContext): CmmnTextAnnotation {

        return CmmnTextAnnotation(
            element.text ?: "",
            element.textFormat
        )
    }

    override fun export(element: CmmnTextAnnotation, context: ExportContext): TTextAnnotation {

        val tTextAnn = TTextAnnotation()

        tTextAnn.text = element.text
        tTextAnn.textFormat = element.textFormat

        return tTextAnn
    }

    override fun getElementType() = TYPE
}
