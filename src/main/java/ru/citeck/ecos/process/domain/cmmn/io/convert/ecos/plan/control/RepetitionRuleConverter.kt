package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control

import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control.RepetitionRuleDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TRepetitionRule

class RepetitionRuleConverter : EcosOmgConverter<RepetitionRuleDef, TRepetitionRule> {

    companion object {
        const val TYPE = "RepetitionRule"
    }

    override fun import(element: TRepetitionRule, context: ImportContext): RepetitionRuleDef {

        return RepetitionRuleDef(element.id)
    }

    override fun export(element: RepetitionRuleDef, context: ExportContext): TRepetitionRule {

        val rule = TRepetitionRule()
        rule.id = element.id

        return rule
    }

    override fun getElementType() = TYPE
}
