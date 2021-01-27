package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.Sentry
import ru.citeck.ecos.process.domain.cmmn.model.TExitCriterion
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnExitCriterion
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnSentryDef
import java.util.*

class ExitCriterionConverter(
    private val converters: CmmnConverters
) : CmmnConverter<TExitCriterion, CmmnExitCriterion> {

    companion object {
        const val TYPE = "ExitCriterion"
    }

    override fun import(element: TExitCriterion, context: ImportContext): CmmnExitCriterion {
        return CmmnExitCriterion(
            element.id,
            MLText(element.name ?: ""),
            converters.import(element.sentryRef as Sentry, CmmnSentryDef::class.java, context).data
        )
    }

    override fun export(element: CmmnExitCriterion, context: ExportContext): TExitCriterion {

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
