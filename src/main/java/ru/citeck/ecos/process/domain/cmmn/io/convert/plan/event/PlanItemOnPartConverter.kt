package ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event

import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.omg.*

class PlanItemOnPartConverter : CmmnConverter<TPlanItemOnPart, PlanItemOnPartDef> {

    companion object {
        const val TYPE = "PlanItemOnPart"
    }

    override fun import(element: TPlanItemOnPart, context: ImportContext): PlanItemOnPartDef {

        val onPartSource = element.sourceRef as TPlanItem
        val exitCriterionRef = (element.exitCriterionRef as? TCriterion)?.id

        return PlanItemOnPartDef(
            element.id,
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
        result.standardEvent = PlanItemTransition.fromValue(element.standardEvent.getValue())
        result.sourceRef = element.sourceRef

        return result
    }

    override fun getElementType(): String = TYPE
}
