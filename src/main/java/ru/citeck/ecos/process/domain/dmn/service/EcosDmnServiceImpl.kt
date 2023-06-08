package ru.citeck.ecos.process.domain.dmn.service

import mu.KotlinLogging
import org.camunda.bpm.engine.DecisionService
import org.springframework.stereotype.Service
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val DEFAULT_OUTPUT_VARIABLE_NAME = "output"

@Service
class EcosDmnServiceImpl(
    private val decisionService: DecisionService
) : EcosDmnService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun evaluateDecisionAndCollectMapEntries(
        decisionRef: EntityRef,
        variables: Map<String, Any?>
    ): Map<String, List<Any>> {

        val decisionKey = decisionRef.toDecisionKey()

        val tableResult = decisionService.evaluateDecisionTableByKey(decisionKey, variables)

        val result = mutableMapOf<String, MutableCollection<Any>>()

        for (resultEntry in tableResult.resultList) {
            for (entry in resultEntry.entries) {
                val outputVariableName = if (entry.key.isNullOrBlank()) {
                    DEFAULT_OUTPUT_VARIABLE_NAME
                } else {
                    entry.key
                }
                result.getOrPut(outputVariableName) { mutableListOf() }.add(entry.value)
            }
        }

        log.debug { "Eval dmn decision: $decisionRef, variables: $variables, result: $result" }

        return result.mapValues { (_, value) -> value.toList() }
    }

    private fun EntityRef.toDecisionKey(): String {
        val keyParts = getLocalId().split(":")
        if (keyParts.size != 3) {
            error("Invalid decision ref: $this")
        }

        val decisionKey = keyParts[0]
        if (decisionKey.isBlank()) {
            error("Decision key can't be blank")
        }

        return decisionKey
    }
}
