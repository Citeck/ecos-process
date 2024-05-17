package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

data class RawFlowElementEvent(
    var processDefinitionId: String? = null,
    var procInstanceId: EntityRef? = null,
    var executionId: String? = null,

    var elementType: String? = null,
    var elementDefId: String? = null,

    var document: EntityRef? = null,

    var time: Instant? = null
)

data class FullFulFlowElementEvent(
    var engine: String? = null,
    var procDefId: String? = null,
    var procDefRef: EntityRef? = null,
    var elementType: String? = null,
    var elementDefId: String? = null,
    var procDeploymentVersion: Int? = null,
    var procInstanceId: EntityRef? = null,
    var processId: String? = null,
    var processRef: EntityRef? = null,
    var executionId: String? = null,
    var document: EntityRef? = null,

    var time: Instant? = null
)
