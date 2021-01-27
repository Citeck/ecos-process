package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event

class CmmnOnPart(
    val id: String,
    val sourceRef: String,
    val eventType: String,
    val exitEventRef: String?
)
