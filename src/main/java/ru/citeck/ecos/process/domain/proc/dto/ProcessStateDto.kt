package ru.citeck.ecos.process.domain.proc.dto

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.*

data class ProcessStateDto(
    val id: @NotNull UUID,
    val data: ByteArray,
    val processId: UUID,
    val created: Instant,
    val version: Int = 0,
    val procDefRevId: UUID
) {
    override fun toString(): String {
        return "ProcessStateDto(id=$id, processId=$processId, created=$created, version=$version, " +
            "procDefRevId=$procDefRevId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessStateDto) return false

        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (processId != other.processId) return false
        if (created != other.created) return false
        if (version != other.version) return false
        if (procDefRevId != other.procDefRevId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + processId.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + version
        result = 31 * result + procDefRevId.hashCode()
        return result
    }
}
