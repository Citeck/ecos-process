package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.control

class PlanItemControlDef(
    val id: String,
    val requiredRule: RequiredRuleDef?,
    val repetitionRule: RepetitionRuleDef?,
    val manualActivationRule: ManualActivationRuleDef?
)
