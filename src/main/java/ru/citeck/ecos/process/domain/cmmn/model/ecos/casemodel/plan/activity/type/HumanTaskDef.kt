package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.type

import ru.citeck.ecos.webapp.api.entity.EntityRef

class HumanTaskDef(
    val roles: List<String>,
    val formRef: EntityRef?
)
