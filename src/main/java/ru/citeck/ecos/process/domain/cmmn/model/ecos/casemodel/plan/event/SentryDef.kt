package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event

import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.OnPartDef

class SentryDef(
    val id: String,
    val onPart: List<OnPartDef>,
    val ifPart: IfPartDef?
)
