package ru.citeck.ecos.process.domain.dmn.service

import ru.citeck.ecos.webapp.api.entity.EntityRef

interface EcosDmnService {

    fun evaluateDecisionAndCollectMapEntries(
        decisionRef: EntityRef,
        variables: Map<String, Any?>
    ): Map<String, List<Any>>

}
