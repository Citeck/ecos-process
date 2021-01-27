package ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.activity.type

class CmmnProcessTask(
    val isBlocking: Boolean = true,
    val processType: String,
    val processDefId: String
)
