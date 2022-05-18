package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

import ecos.com.fasterxml.jackson210.core.JsonGenerator
import ecos.com.fasterxml.jackson210.core.JsonParser
import ecos.com.fasterxml.jackson210.databind.DeserializationContext
import ecos.com.fasterxml.jackson210.databind.JsonDeserializer
import ecos.com.fasterxml.jackson210.databind.JsonSerializer
import ecos.com.fasterxml.jackson210.databind.SerializerProvider
import ecos.com.fasterxml.jackson210.databind.annotation.JsonDeserialize
import ecos.com.fasterxml.jackson210.databind.annotation.JsonSerialize

@JsonSerialize(using = OutcomeSerializer::class)
@JsonDeserialize(using = OutcomeDeserializer::class)
class Outcome(
    val data: String
) {
    var id: String = ""
    var value: String = ""

    companion object {
        private const val SEPARATOR = ":"
        const val OUTCOME_VAR = "outcome"
        const val OUTCOME_PREFIX = OUTCOME_VAR + "_"

        val EMPTY = Outcome("")
    }

    constructor(id: String, value: String) : this("$id$SEPARATOR$value")

    init {
        if (data.isNotBlank()) {
            val split = data.split(SEPARATOR)

            if (split.size != 2) throw IllegalStateException("Outcome format is invalid")

            id = split[0]
            value = split[1]

            if (id.isBlank()) throw IllegalStateException("Outcome id cannot be blank")
            if (value.isBlank()) throw IllegalStateException("Outcome value cannot be blank")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Outcome) return false

        if (id != other.id) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        if (this == EMPTY) {
            return ""
        }
        return id + SEPARATOR + value
    }

}

object OutcomeSerializer : JsonSerializer<Outcome>() {
    override fun serialize(value: Outcome, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(value.toString())
    }
}

object OutcomeDeserializer : JsonDeserializer<Outcome>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Outcome {
        return Outcome(p.text)
    }

}
