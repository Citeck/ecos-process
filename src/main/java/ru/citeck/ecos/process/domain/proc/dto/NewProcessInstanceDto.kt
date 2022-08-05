package ru.citeck.ecos.process.domain.proc.dto

import java.util.*

data class NewProcessInstanceDto(
    val id: UUID,
    val stateId: UUID,
    val stateData: ByteArray
) {
    override fun toString(): String {
        return "NewProcessInstanceDto(id=$id, stateId=$stateId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewProcessInstanceDto) return false

        if (id != other.id) return false
        if (stateId != other.stateId) return false
        if (!stateData.contentEquals(other.stateData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + stateId.hashCode()
        result = 31 * result + stateData.contentHashCode()
        return result
    }
}
