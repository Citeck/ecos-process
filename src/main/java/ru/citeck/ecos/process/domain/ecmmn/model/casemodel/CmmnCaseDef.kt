package ru.citeck.ecos.process.domain.ecmmn.model.casemodel

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.CmmnPlanModelDef

class CmmnCaseDef(
    val id: String,
    val name: MLText,
    val planModel: CmmnPlanModelDef
)
