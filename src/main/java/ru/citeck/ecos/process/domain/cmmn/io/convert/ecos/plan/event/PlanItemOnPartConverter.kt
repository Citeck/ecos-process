package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.omg.PlanItemTransition
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCriterion
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItem
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItemOnPart
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class PlanItemOnPartConverter : EcosOmgConverter<PlanItemOnPartDef, TPlanItemOnPart> {

    override fun import(element: TPlanItemOnPart, context: ImportContext): PlanItemOnPartDef {

        val onPartSource = element.sourceRef as TPlanItem
        val exitCriterionRef = (element.exitCriterionRef as? TCriterion)?.id
        val name = element.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: element.name

        return PlanItemOnPartDef(
            element.id,
            Json.mapper.read(name, MLText::class.java) ?: MLText(),
            onPartSource.id,
            PlanItemTransitionEnum.fromValue(element.standardEvent.value()),
            exitCriterionRef
        )
    }

    override fun export(element: PlanItemOnPartDef, context: ExportContext): TPlanItemOnPart {

        val result = TPlanItemOnPart()

        if (element.exitCriterionRef != null) {
            result.exitCriterionRef = element.exitCriterionRef
        }
        result.id = element.id
        result.name = MLText.getClosestValue(element.name, I18nContext.getLocale())
        result.standardEvent = PlanItemTransition.fromValue(element.standardEvent.getValue())
        result.sourceRef = element.sourceRef

        result.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(element.name)

        return result
    }
}
