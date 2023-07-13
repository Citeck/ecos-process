package ru.citeck.ecos.process.common

data class CamundaEntityId(
    val key: String,
    val version: Int,
    val id: String
) {
    init {
        if (key.isBlank()) {
            error("EntityId key can't be blank: $this")
        }

        if (version < 0) {
            error("EntityId version can't be negative: $this")
        }

        if (id.isBlank()) {
            error("EntityId id can't be blank: $this")
        }
    }
}
