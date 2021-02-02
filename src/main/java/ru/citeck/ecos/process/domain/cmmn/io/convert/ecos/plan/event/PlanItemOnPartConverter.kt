package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.records3.record.request.RequestContext

class PlanItemOnPartConverter: EcosOmgConverter<PlanItemOnPartDef, TPlanItemOnPart> {

    companion object {
        const val TYPE = "PlanItemOnPart"
    }

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
        result.name = MLText.getClosestValue(element.name, RequestContext.getLocale())
        result.standardEvent = PlanItemTransition.fromValue(element.standardEvent.getValue())
        result.sourceRef = element.sourceRef

        result.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(element.name)

        return result
    }

    override fun getElementType(): String = TYPE
}
