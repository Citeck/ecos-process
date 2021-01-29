package ru.citeck.ecos.process.domain.procdef.dto

import org.apache.commons.lang3.StringUtils

class ProcDefRef private constructor(
    val type: String,
    val id: String
) {
    companion object {

        @JvmField
        val EMPTY = ProcDefRef("", "")

        private const val TYPE_DELIMITER = "$"

        @JvmStatic
        fun create(type: String, id: String): ProcDefRef {
            return ProcDefRef(type, id)
        }

        @JvmStatic
        fun valueOf(str: String): ProcDefRef {

            if (StringUtils.isBlank(str) || TYPE_DELIMITER == str) {
                return EMPTY
            }
            val delimIdx = str.indexOf(TYPE_DELIMITER)
            val type = str.substring(0, delimIdx)
            val id: String
            id = if (delimIdx == str.length - 1) {
                StringUtils.EMPTY
            } else {
                str.substring(delimIdx + 1)
            }
            return create(type, id)
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

        if (type != other.type || id != other.id) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    override fun toString(): String {
        return if (this == EMPTY) {
            ""
        } else {
            "$type$TYPE_DELIMITER$id"
        }
    }
}
