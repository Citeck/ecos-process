package ru.citeck.ecos.process.domain.cmmn.io.convert.plan

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.EntryCriterionConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.ExitCriterionConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.EntryCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import java.util.*

class ActivityConverter(private val converters: CmmnConverters) : CmmnConverter<TPlanItem, ActivityDef> {

    companion object {
        const val TYPE = "Activity"
    }

    override fun import(element: TPlanItem, context: ImportContext): ActivityDef {

        val activityDef = ActivityDef.create()
        activityDef.planItemId = element.id

        if (element.itemControl != null) {
            if (element.itemControl.manualActivationRule != null) {
                activityDef.manualActivationRule = true
            }
            if (element.itemControl.repetitionRule != null) {
                activityDef.repetitionRule = true
            }
            if (element.itemControl.requiredRule != null) {
                activityDef.requiredRule = true
            }
        }

        val definition = element.definitionRef

        if (definition is TPlanItemDefinition) {
            activityDef.id = definition.id
            activityDef.name = MLText(definition.name ?: "")
        } else {
            error("Unsupported type: " + definition::class)
        }

        val data = converters.import(definition, context)
        activityDef.data = data.data
        activityDef.type = data.type

        if (element.entryCriterion != null) {
            activityDef.entryCriteria = element.entryCriterion.filter {
                isValidCriterion(it)
            }.map {
                converters.import(it, EntryCriterionDef::class.java, context).data
            }
        }
        if (element.exitCriterion != null) {
            activityDef.exitCriteria = element.exitCriterion.filter {
                isValidCriterion(it)
            }.map {
                converters.import(it, ExitCriterionDef::class.java, context).data
            }
        }

        return activityDef.build()
    }

    private fun isValidCriterion(criterion: Any?): Boolean {

        val tCriterion = criterion as? TCriterion ?: return false
        tCriterion.sentryRef ?: return false

        return true
    }

    override fun export(element: ActivityDef, context: ExportContext): TPlanItem {

        val definition = converters.export<TPlanItemDefinition>(element.type, element.data, context)

        val planItem = TPlanItem()
        planItem.id = element.planItemId
        planItem.definitionRef = definition

        if (element.manualActivationRule == true) {
            planItem.itemControl = TPlanItemControl()
            planItem.itemControl.manualActivationRule = TManualActivationRule()
            planItem.itemControl.manualActivationRule.id = CmmnXmlUtils.generateId("ManualActivationRule")
        }
        if (element.repetitionRule == true) {
            if (planItem.itemControl == null) {
                planItem.itemControl = TPlanItemControl()
            }
            planItem.itemControl.repetitionRule = TRepetitionRule()
            planItem.itemControl.repetitionRule.id = CmmnXmlUtils.generateId("RepetitionRule")
        }
        if (element.requiredRule == true) {
            if (planItem.itemControl == null) {
                planItem.itemControl = TPlanItemControl()
            }
            planItem.itemControl.requiredRule = TRequiredRule()
            planItem.itemControl.requiredRule.id = CmmnXmlUtils.generateId("RequiredRule")
        }

        exportSentries(planItem, element, context)

        definition.id = element.id

        val name = MLText.getClosestValue(element.name, Locale.ENGLISH)
        if (name.isNotBlank()) {
            definition.name = name
        }

        context.elementsById[planItem.id] = planItem
        context.elementsById[definition.id] = definition

        return planItem
    }

    private fun exportSentries(
        planItem: TPlanItem,
        activity: ActivityDef,
        context: ExportContext
    ) {
        activity.entryCriteria.forEach {
            planItem.entryCriterion.add(converters.export(EntryCriterionConverter.TYPE, it, context))
        }
        activity.exitCriteria.forEach {
            planItem.exitCriterion.add(converters.export(ExitCriterionConverter.TYPE, it, context))
        }
    }

    override fun getElementType() = TYPE
}
