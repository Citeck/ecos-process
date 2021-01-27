package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type

import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.CmmnActivityDef

class CmmnStage(
    val autoComplete: Boolean,
    val children: List<CmmnActivityDef>
)
