package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.EntryCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.SentryDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.Sentry
import ru.citeck.ecos.process.domain.cmmn.model.omg.TEntryCriterion
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import java.util.*

class EntryCriterionConverter : EcosOmgConverter<EntryCriterionDef, TEntryCriterion> {

    override fun import(element: TEntryCriterion, context: ImportContext): EntryCriterionDef {
        return EntryCriterionDef(
            element.id,
            MLText(element.name ?: ""),
            context.converters.import(element.sentryRef as Sentry, SentryDef::class.java, context).data
        )
    }

    override fun export(element: EntryCriterionDef, context: ExportContext): TEntryCriterion {

        val result = TEntryCriterion()
        result.id = element.id
        result.sentryRef = context.converters.export<Sentry>(element.sentry, context)

        val name = MLText.getClosestValue(element.name, Locale.ENGLISH)
        if (name.isNotBlank()) {
            result.name = name
        }

        context.cmmnElementsById[result.id] = result

        return result
    }
}
