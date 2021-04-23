package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.ExpressionDef

class IfPartDef(
    val id: String,
    val condition: ExpressionDef
)
