package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type

import ru.citeck.ecos.records2.RecordRef

class HumanTaskDef(
    val roles: List<String>,
    val formRef: RecordRef?
)
