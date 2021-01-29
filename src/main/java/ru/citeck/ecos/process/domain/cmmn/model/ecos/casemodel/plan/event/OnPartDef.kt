package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event

class OnPartDef(
    val id: String,
    val sourceRef: String,
    val eventType: String,
    val exitEventRef: String?
)
