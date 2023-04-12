package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

import ecos.com.fasterxml.jackson210.core.JsonGenerator
import ecos.com.fasterxml.jackson210.core.JsonParser
import ecos.com.fasterxml.jackson210.databind.DeserializationContext
import ecos.com.fasterxml.jackson210.databind.JsonDeserializer
import ecos.com.fasterxml.jackson210.databind.JsonSerializer
import ecos.com.fasterxml.jackson210.databind.SerializerProvider
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ecos.com.fasterxml.jackson210.databind.annotation.JsonSerialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException

@JsonSerialize(using = OutcomeSerializer::class)
@JsonDeserialize(using = OutcomeDeserializer::class)
class Outcome(
    val data: String,
    val name: MLText = MLText()
) {
    var taskDefinitionKey: String = ""
    var value: String = ""

    val outcomeId = fun(): String {
        return taskDefinitionKey + "_" + OUTCOME_VAR
    }

    val nameId = fun(): String {
        return taskDefinitionKey + "_" + OUTCOME_NAME_VAR
    }

    companion object {
        private const val SEPARATOR = ":"
        const val OUTCOME_VAR = "outcome"
        const val OUTCOME_NAME_VAR = "outcomeName"

        const val OUTCOME_PREFIX = OUTCOME_VAR + "_"

        const val OUTCOME_POSTFIX = "_$OUTCOME_VAR"
        const val OUTCOME_NAME_POSTFIX = "_$OUTCOME_NAME_VAR"

        val EMPTY = Outcome("")
    }

    constructor(taskDefinitionKey: String, value: String, name: MLText) : this("$taskDefinitionKey$SEPARATOR$value", name)

    init {
        if (data.isNotBlank()) {
            val split = data.split(SEPARATOR)

            if (split.size != 2) throw EcosBpmnDefinitionException("Outcome format is invalid")

            taskDefinitionKey = split[0]
            value = split[1]

            if (taskDefinitionKey.isBlank()) throw EcosBpmnDefinitionException("Outcome taskDefinitionKey cannot be blank")
            if (value.isBlank()) throw EcosBpmnDefinitionException("Outcome value cannot be blank")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Outcome) return false

        if (taskDefinitionKey != other.taskDefinitionKey) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = taskDefinitionKey.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        if (this == EMPTY) {
            return ""
        }
        return taskDefinitionKey + SEPARATOR + value
    }
}

class OutcomeSerializer : JsonSerializer<Outcome>() {
    override fun serialize(value: Outcome, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(value.toString())
    }
}

class OutcomeDeserializer : JsonDeserializer<Outcome>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Outcome {
        return Outcome(p.text)
    }
}
