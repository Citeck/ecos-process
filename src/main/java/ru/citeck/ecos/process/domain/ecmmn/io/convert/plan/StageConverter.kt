package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan

import ru.citeck.ecos.process.domain.cmmn.model.*
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.CmmnActivityDef
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type.CmmnStage

class StageConverter(
    private val converters: CmmnConverters
) : CmmnConverter<Stage, CmmnStage> {

    companion object {
        const val TYPE = "Stage"
    }

    override fun import(element: Stage, context: ImportContext): CmmnStage {

        return CmmnStage(
            element.isAutoComplete,
            element.planItem.map {
                converters.import(it, CmmnActivityDef::class.java, context).data
            }
        )
    }

    override fun export(element: CmmnStage, context: ExportContext): Stage {

        val stage = Stage()
        if (element.autoComplete) {
            stage.isAutoComplete = true
        }

        element.children.map {
            converters.export<TPlanItem>(ActivityConverter.TYPE, it, context)
        }.forEach { planItem ->
            stage.planItem.add(planItem)
            stage.planItemDefinition.add(converters.convertToJaxb(planItem.definitionRef as TPlanItemDefinition))
            planItem.entryCriterion?.mapNotNull { it.sentryRef as? Sentry }?.forEach { stage.sentry.add(it) }
            planItem.exitCriterion?.mapNotNull { it.sentryRef as? Sentry }?.forEach { stage.sentry.add(it) }
        }
        return stage
    }

    override fun getElementType() = TYPE
}
