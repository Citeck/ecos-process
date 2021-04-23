package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event

import ru.citeck.ecos.commons.data.MLText

class ExitCriterionDef(
    val id: String,
    val name: MLText,
    val sentry: SentryDef
)
