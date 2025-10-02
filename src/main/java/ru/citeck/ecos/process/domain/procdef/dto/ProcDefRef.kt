package ru.citeck.ecos.process.domain.procdef.dto

import ru.citeck.ecos.model.lib.workspace.IdInWs

class ProcDefRef private constructor(
    val type: String,
    val idInWs: IdInWs
) {
    companion object {

        @JvmField
        val EMPTY = ProcDefRef("", IdInWs.EMPTY)

        private const val TYPE_DELIMITER = "$"

        @JvmStatic
        fun create(type: String, id: IdInWs): ProcDefRef {
            return ProcDefRef(type, id)
        }

        fun createWoWs(type: String, id: String): ProcDefRef {
            return ProcDefRef(type, IdInWs.create(id))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as ProcDefRef

        if (type != other.type || idInWs != other.idInWs) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + idInWs.hashCode()
        return result
    }

    override fun toString(): String {
        return if (this == EMPTY) {
            ""
        } else {
            "$type$TYPE_DELIMITER$idInWs"
        }
    }
}
