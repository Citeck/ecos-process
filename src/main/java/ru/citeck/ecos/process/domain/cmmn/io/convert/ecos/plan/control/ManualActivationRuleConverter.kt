package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control

import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.ManualActivationRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TManualActivationRule

class ManualActivationRuleConverter : EcosOmgConverter<ManualActivationRuleDef, TManualActivationRule> {

    companion object {
        const val TYPE = "ManualActivationRule"
    }

    override fun import(element: TManualActivationRule, context: ImportContext): ManualActivationRuleDef {

        return ManualActivationRuleDef(element.id)
    }

    override fun export(element: ManualActivationRuleDef, context: ExportContext): TManualActivationRule {

        val rule = TManualActivationRule()
        rule.id = element.id

        return rule
    }

    override fun getElementType() = TYPE
}
