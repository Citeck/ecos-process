package ru.citeck.ecos.process.domain.dmn.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.DecisionService
import org.springframework.stereotype.Service

private const val DEFAULT_OUTPUT_VARIABLE_NAME = "output"

@Service
class EcosDmnServiceImpl(
    private val decisionService: DecisionService
) : EcosDmnService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun evaluateDecisionByKeyAndCollectMapEntries(
        key: String,
        variables: Map<String, Any?>
    ): Map<String, List<Any>> {

        val tableResult = decisionService.evaluateDecisionTableByKey(key, variables)

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

        log.debug { "Eval dmn decision by key: $key, variables: $variables, result: $result" }

        return result.mapValues { (_, value) -> value.toList() }
    }
}
