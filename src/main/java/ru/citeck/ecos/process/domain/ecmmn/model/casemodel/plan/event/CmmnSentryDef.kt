package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event

import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.CmmnExpressionDef

class CmmnSentryDef(
    val id: String,
    val onPart: List<CmmnOnPart>,
    val ifPart: CmmnExpressionDef?
)
