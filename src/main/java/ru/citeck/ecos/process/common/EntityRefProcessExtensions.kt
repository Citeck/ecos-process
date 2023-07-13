package ru.citeck.ecos.process.common

import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDecisionRecords
import ru.citeck.ecos.webapp.api.entity.EntityRef

internal fun EntityRef.toDecisionId(): CamundaEntityId {
    val keyParts = getLocalId().split(":")
    if (keyParts.size != 3) {
        error("Invalid decision ref: $this")
    }

    return CamundaEntityId(
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

internal fun EntityRef.toProcessKey(): String {
    return if (getSourceId() == BpmnProcessRecords.ID) {
        toDecisionId().key
    } else {
        getLocalId()
    }
}
