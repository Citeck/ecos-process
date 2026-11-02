package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import ru.citeck.ecos.records2.predicate.model.*
import java.util.Locale

/**
 * Generates SQL from known columns and operators only.
 * Every user value, including a deployment resource name, is a bound MyBatis parameter.
 */
class BpmnProcessLatestFilter private constructor(
    val sql: String,
    val params: List<String>
) {

    companion object {

        private const val TRUE_CONDITION = "1 = 1"
        private const val FALSE_CONDITION = "1 = 0"

        fun create(
            predicate: Predicate,
            definitionResources: (ValuePredicate) -> List<String> = { emptyList() }
        ): BpmnProcessLatestFilter {

            return Compiler(definitionResources).build(predicate)
        }
    }

    private class Compiler(
        private val definitionResources: (ValuePredicate) -> List<String>
    ) {

        private val params = mutableListOf<String>()

        fun build(predicate: Predicate): BpmnProcessLatestFilter {

            val sql = compile(predicate)
            return BpmnProcessLatestFilter(sql, params.toList())
        }

        private fun compile(predicate: Predicate): String {

            return when (predicate) {
                is VoidPredicate -> TRUE_CONDITION
                is AndPredicate -> compileGroup(predicate.getPredicates(), "AND", TRUE_CONDITION)
                is OrPredicate -> compileGroup(predicate.getPredicates(), "OR", FALSE_CONDITION)
                is NotPredicate -> "NOT (${compile(predicate.getPredicate())})"
                is ValuePredicate -> compileValue(predicate)
                else -> error("Unsupported process filter: $predicate")
            }
        }

        private fun compileGroup(
            predicates: List<Predicate>,
            operator: String,
            emptyCondition: String
        ): String {

            if (predicates.isEmpty()) {
                return emptyCondition
            }

            return predicates.joinToString(" $operator ", "(", ")") { compile(it) }
        }

        private fun compileValue(predicate: ValuePredicate): String {

            return when (predicate.getAttribute()) {
                "id" -> compileId(predicate)
                "definition", "name", "_disp" -> compileDefinition(predicate)
                "_type" -> compileType(predicate)
                else -> error("Unsupported process filter attribute: ${predicate.getAttribute()}")
            }
        }

        private fun compileId(predicate: ValuePredicate): String {

            val value = predicate.getValue().asText().lowercase(Locale.ROOT)

            return when (predicate.getType()) {
                ValuePredicate.Type.CONTAINS -> {
                    val escaped = value
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")

                    "LOWER(DEF.KEY_) LIKE ${bind("%$escaped%")} ESCAPE '!'"
                }

                ValuePredicate.Type.LIKE -> "LOWER(DEF.KEY_) LIKE ${bind(value)} ESCAPE '!'"

                ValuePredicate.Type.IN -> {
                    val values = predicate.getValue().asList(String::class.java)
                    if (values.isEmpty()) {
                        FALSE_CONDITION
                    } else {
                        val placeholders = values.joinToString { bind(it.lowercase(Locale.ROOT)) }
                        "LOWER(DEF.KEY_) IN ($placeholders)"
                    }
                }

                else -> {
                    val operator = when (predicate.getType()) {
                        ValuePredicate.Type.EQ -> "="
                        ValuePredicate.Type.GT -> ">"
                        ValuePredicate.Type.GE -> ">="
                        ValuePredicate.Type.LT -> "<"
                        ValuePredicate.Type.LE -> "<="
                        else -> error("Unsupported process filter: $predicate")
                    }

                    "LOWER(DEF.KEY_) $operator ${bind(value)}"
                }
            }
        }

        private fun compileDefinition(predicate: ValuePredicate): String {

            val resources = definitionResources(predicate)
            if (resources.isEmpty()) {
                return FALSE_CONDITION
            }

            val placeholders = resources.joinToString { bind(it) }
            return "DEF.RESOURCE_NAME_ IN ($placeholders)"
        }

        private fun compileType(predicate: ValuePredicate): String {

            require(predicate.getType() == ValuePredicate.Type.EQ) {
                "Unsupported type filter: $predicate"
            }

            val typeId = predicate.getValue().asText().substringAfterLast('@')
            return if (typeId == "bpmn-proc-latest") TRUE_CONDITION else FALSE_CONDITION
        }

        private fun bind(value: String): String {

            params.add(value)
            return "#{filter.params[${params.lastIndex}]}"
        }
    }
}
