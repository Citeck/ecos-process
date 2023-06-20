package ru.citeck.ecos.process.domain.dmn.dto

import ru.citeck.ecos.process.domain.dmn.api.records.DmnDecisionRecords
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class DecisionId(
    val key: String,
    val version: Int,
    val id: String
) {
    init {
        if (key.isBlank()) {
            error("Decision key can't be blank")
        }

        if (version < 0) {
            error("Decision version can't be negative")
        }

        if (id.isBlank()) {
            error("Decision id can't be blank")
        }
    }
}

internal fun EntityRef.toDecisionId(): DecisionId {
    val keyParts = getLocalId().split(":")
    if (keyParts.size != 3) {
        error("Invalid decision ref: $this")
    }

    return DecisionId(
        key = keyParts[0],
        version = keyParts[1].toInt(),
        id = keyParts[2]
    )
}

internal fun EntityRef.toDecisionKey(): String {
    return if (getSourceId() == DmnDecisionRecords.ID) {
        toDecisionId().key
    } else {
        getLocalId()
    }
}
