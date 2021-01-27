package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.CmmnActivityDef
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnExitCriterion

class CmmnPlanModelDef(
    val id: String,
    val name: MLText,
    val exitCriteria: List<CmmnExitCriterion>,
    val children: List<CmmnActivityDef>
)
