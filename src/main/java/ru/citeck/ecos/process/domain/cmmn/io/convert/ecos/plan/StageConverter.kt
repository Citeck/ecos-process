package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type.StageDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.Sentry
import ru.citeck.ecos.process.domain.cmmn.model.omg.Stage
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItem
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItemDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class StageConverter : EcosOmgConverter<StageDef, Stage> {

    override fun import(element: Stage, context: ImportContext): StageDef {

        return StageDef(
            element.isAutoComplete,
            element.planItem.map {
                context.converters.import(it, ActivityDef::class.java, context).data
            }
        )
    }

    override fun export(element: StageDef, context: ExportContext): Stage {

        val stage = Stage()
        if (element.autoComplete) {
            stage.isAutoComplete = true
        }

        element.children.map {
            context.converters.export<TPlanItem>(it, context)
        }.forEach { planItem ->
            stage.planItem.add(planItem)
            stage.planItemDefinition.add(context.converters.convertToJaxb(planItem.definitionRef as TPlanItemDefinition))
            planItem.entryCriterion?.mapNotNull { it.sentryRef as? Sentry }?.forEach { stage.sentry.add(it) }
            planItem.exitCriterion?.mapNotNull { it.sentryRef as? Sentry }?.forEach { stage.sentry.add(it) }
        }
        return stage
    }
}
