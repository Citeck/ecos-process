package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type

class ProcessTaskDef(
    val isBlocking: Boolean = true,
    val processType: String,
    val processDefId: String
)
