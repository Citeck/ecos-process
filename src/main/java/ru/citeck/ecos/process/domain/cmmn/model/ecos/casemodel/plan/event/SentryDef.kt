package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.ExpressionDef

class SentryDef(
    val id: String,
    val onPart: List<OnPartDef>,
    val ifPart: ExpressionDef?
)
