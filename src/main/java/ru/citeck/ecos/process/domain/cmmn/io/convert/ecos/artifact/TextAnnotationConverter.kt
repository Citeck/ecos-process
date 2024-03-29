package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.TextAnnotationDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TTextAnnotation
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class TextAnnotationConverter : EcosOmgConverter<TextAnnotationDef, TTextAnnotation> {

    companion object {
        val PROP_TEXT_ML = QName(CmmnXmlUtils.NS_ECOS, "text_ml")
    }

    override fun import(element: TTextAnnotation, context: ImportContext): TextAnnotationDef {

        val text = element.otherAttributes[PROP_TEXT_ML] ?: element.text

        return TextAnnotationDef(
            Json.mapper.read(text, MLText::class.java) ?: MLText(),
            element.textFormat
        )
    }

    override fun export(element: TextAnnotationDef, context: ExportContext): TTextAnnotation {

        val tTextAnn = TTextAnnotation()

        tTextAnn.text = MLText.getClosestValue(element.text, I18nContext.getLocale())
        tTextAnn.textFormat = element.textFormat

        tTextAnn.otherAttributes[PROP_TEXT_ML] = Json.mapper.toString(element.text)

        return tTextAnn
    }
}
