package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control

import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.RequiredRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TRequiredRule

class RequiredRuleConverter : EcosOmgConverter<RequiredRuleDef, TRequiredRule> {

    companion object {
        const val TYPE = "RequiredRule"
    }

    override fun import(element: TRequiredRule, context: ImportContext): RequiredRuleDef {

        return RequiredRuleDef(element.id)
    }

    override fun export(element: RequiredRuleDef, context: ExportContext): TRequiredRule {

        val rule = TRequiredRule()
        rule.id = element.id

        return rule
    }

    override fun getElementType() = TYPE
}
