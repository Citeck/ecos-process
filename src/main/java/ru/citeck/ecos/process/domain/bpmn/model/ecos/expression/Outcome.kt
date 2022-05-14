package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

class Outcome(
    val data: String
) {
    var id: String = ""
    var key: String = ""

    companion object {
        private const val SEPARATOR = ":"
        const val OUTCOME_POSTFIX = "outcome"

        val EMPTY = Outcome("")
    }

    init {
        if (data.isNotBlank()) {
            val split = data.split(SEPARATOR)

            if (split.size != 2) throw IllegalStateException("Outcome format is invalid")

            id = split[0]
            key = split[1]

            if (id.isBlank()) throw IllegalStateException("Outcome id cannot be blank")
            if (key.isBlank()) throw IllegalStateException("Outcome key cannot be blank")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Outcome) return false

        if (id != other.id) return false
        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }

    override fun toString(): String {
        if (this == EMPTY) {
            return ""
        }
        return id + SEPARATOR + key
    }

}
