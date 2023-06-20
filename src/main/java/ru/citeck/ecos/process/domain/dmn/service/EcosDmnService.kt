package ru.citeck.ecos.process.domain.dmn.service

interface EcosDmnService {
    fun evaluateDecisionByKeyAndCollectMapEntries(
        key: String,
        variables: Map<String, Any?>
    ): Map<String, List<Any>>
}
