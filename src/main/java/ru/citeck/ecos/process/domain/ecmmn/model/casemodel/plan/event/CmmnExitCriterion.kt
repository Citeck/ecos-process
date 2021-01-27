package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event

import ru.citeck.ecos.commons.data.MLText

class CmmnExitCriterion(
    val id: String,
    val name: MLText,
    val sentry: CmmnSentryDef
)
