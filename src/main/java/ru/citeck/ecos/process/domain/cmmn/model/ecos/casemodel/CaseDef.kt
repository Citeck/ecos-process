package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.PlanModelDef

class CaseDef(
    val id: String,
    val name: MLText,
    val planModel: PlanModelDef
)
