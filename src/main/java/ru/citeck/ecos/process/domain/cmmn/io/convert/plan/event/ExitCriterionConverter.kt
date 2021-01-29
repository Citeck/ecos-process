package ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.omg.Sentry
import ru.citeck.ecos.process.domain.cmmn.model.omg.TExitCriterion
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.SentryDef
import java.util.*

class ExitCriterionConverter(
    private val converters: CmmnConverters
) : CmmnConverter<TExitCriterion, ExitCriterionDef> {

    companion object {
        const val TYPE = "ExitCriterion"
    }

    override fun import(element: TExitCriterion, context: ImportContext): ExitCriterionDef {
        return ExitCriterionDef(
            element.id,
            MLText(element.name ?: ""),
            converters.import(element.sentryRef as Sentry, SentryDef::class.java, context).data
        )
    }

    override fun export(element: ExitCriterionDef, context: ExportContext): TExitCriterion {

        val result = TExitCriterion()
        result.id = element.id
        result.sentryRef = converters.export<Sentry>(SentryConverter.TYPE, element.sentry, context)

        val name = MLText.getClosestValue(element.name, Locale.ENGLISH)
        if (name.isNotBlank()) {
            result.name = name
        }
        context.elementsById[result.id] = result

        return result
    }

    override fun getElementType() = TYPE
}
