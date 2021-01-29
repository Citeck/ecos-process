package ru.citeck.ecos.process.domain.cmmn.io.convert.plan

import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.StageDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.Sentry
import ru.citeck.ecos.process.domain.cmmn.model.omg.Stage
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItem
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItemDefinition

class StageConverter(
    private val converters: CmmnConverters
) : CmmnConverter<Stage, StageDef> {

    companion object {
        const val TYPE = "Stage"
    }

    override fun import(element: Stage, context: ImportContext): StageDef {

        return StageDef(
            element.isAutoComplete,
            element.planItem.map {
                converters.import(it, ActivityDef::class.java, context).data
            }
        )
    }

    override fun export(element: StageDef, context: ExportContext): Stage {

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
