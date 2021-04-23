package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef

class PlanModelDef(
    val id: String,
    val name: MLText,
    val exitCriteria: List<ExitCriterionDef>,
    val children: List<ActivityDef>
)
