package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef

class StageDef(
    val autoComplete: Boolean = true,
    val children: List<ActivityDef>
)
