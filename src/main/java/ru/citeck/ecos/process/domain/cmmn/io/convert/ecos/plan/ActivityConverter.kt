package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.PlanItemControlDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.EntryCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCriterion
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItem
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItemDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class ActivityConverter : EcosOmgConverter<ActivityDef, TPlanItem> {

    override fun import(element: TPlanItem, context: ImportContext): ActivityDef {

        val activityDef = ActivityDef.create()
        activityDef.planItemId = element.id

        val control = element.itemControl
        if (control != null) {
            activityDef.withControl(context.converters.import(control, PlanItemControlDef::class.java, context).data)
        }

        val definition = element.definitionRef
        if (definition is TPlanItemDefinition) {
            activityDef.id = definition.id
            val name = definition.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: definition.name
            activityDef.name = Json.mapper.read(name, MLText::class.java) ?: MLText()
        } else {
            error("Unsupported type: " + definition::class)
        }

        val data = context.converters.import(definition, context)
        activityDef.data = data.data
        activityDef.type = data.type

        if (element.entryCriterion != null) {
            activityDef.entryCriteria = element.entryCriterion.filter {
                isValidCriterion(it)
            }.map {
                context.converters.import(it, EntryCriterionDef::class.java, context).data
            }
        }
        if (element.exitCriterion != null) {
            activityDef.exitCriteria = element.exitCriterion.filter {
                isValidCriterion(it)
            }.map {
                context.converters.import(it, ExitCriterionDef::class.java, context).data
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

        val definition = context.converters.export<TPlanItemDefinition>(element.type, element.data, context)
        if (!element.type.startsWith("cmmn:")) {
            definition.otherAttributes[CmmnXmlUtils.PROP_ECOS_CMMN_TYPE] = element.type
        }

        val planItem = TPlanItem()
        planItem.id = element.planItemId
        planItem.definitionRef = definition

        val control = element.control
        if (control != null) {
            planItem.itemControl = context.converters.export(control, context)
        }

        exportSentries(planItem, element, context)

        definition.id = element.id

        definition.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(element.name)
        val name = MLText.getClosestValue(element.name, I18nContext.getLocale())
        if (name.isNotBlank()) {
            definition.name = name
        }

        context.cmmnElementsById[planItem.id] = planItem
        context.cmmnElementsById[definition.id] = definition
        context.cmmnPItemByDefId[definition.id] = planItem

        return planItem
    }

    private fun exportSentries(
        planItem: TPlanItem,
        activity: ActivityDef,
        context: ExportContext
    ) {
        activity.entryCriteria.forEach {
            planItem.entryCriterion.add(context.converters.export(it, context))
        }
        activity.exitCriteria.forEach {
            planItem.exitCriterion.add(context.converters.export(it, context))
        }
    }
}
