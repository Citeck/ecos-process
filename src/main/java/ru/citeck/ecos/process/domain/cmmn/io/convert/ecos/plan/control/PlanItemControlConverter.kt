package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control

import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.ManualActivationRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.PlanItemControlDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.RepetitionRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.RequiredRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TPlanItemControl

class PlanItemControlConverter: EcosOmgConverter<PlanItemControlDef, TPlanItemControl> {

    companion object {
        const val TYPE = "PlanItemControl"
    }

    override fun import(element: TPlanItemControl, context: ImportContext): PlanItemControlDef {

        return PlanItemControlDef(
            element.id,
            element.requiredRule?.let {
                context.converters.import(it, RequiredRuleDef::class.java, context).data
            },
            element.repetitionRule?.let {
                context.converters.import(it, RepetitionRuleDef::class.java, context).data
            },
            element.manualActivationRule?.let {
                context.converters.import(it, ManualActivationRuleDef::class.java, context).data
            }
        )
    }

    override fun export(element: PlanItemControlDef, context: ExportContext): TPlanItemControl {

        val control = TPlanItemControl()
        control.id = element.id

        if (element.manualActivationRule != null) {
            control.manualActivationRule = context.converters.export(
                ManualActivationRuleConverter.TYPE,
                element.manualActivationRule,
                context
            )
        }
        if (element.repetitionRule != null) {
            control.repetitionRule = context.converters.export(
                RepetitionRuleConverter.TYPE,
                element.repetitionRule,
                context
            )
        }
        if (element.requiredRule != null) {
            control.requiredRule = context.converters.export(
                RequiredRuleConverter.TYPE,
                element.requiredRule,
                context
            )
        }

        return control
    }

    override fun getElementType() = TYPE
}
