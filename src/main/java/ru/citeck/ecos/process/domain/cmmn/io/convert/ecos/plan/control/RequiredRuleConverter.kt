package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.RequiredRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TRequiredRule
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class RequiredRuleConverter : EcosOmgConverter<RequiredRuleDef, TRequiredRule> {

    override fun import(element: TRequiredRule, context: ImportContext): RequiredRuleDef {

        return RequiredRuleDef(element.id)
    }

    override fun export(element: RequiredRuleDef, context: ExportContext): TRequiredRule {

        val rule = TRequiredRule()
        rule.id = element.id

        return rule
    }
}
