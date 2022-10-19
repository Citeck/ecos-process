package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class FlowElementEvent(
    var engine: String? = null,
    var procDefId: String? = null,
    var elementType: String? = null,
    var elementDefId: String? = null,
    var procDeploymentVersion: Int? = null,
    var procInstanceId: EntityRef? = null,
    var executionId: String? = null,
    var document: RecordRef? = null,

    @AttName("\$event.time")
    var time: String? = null,

    @AttName("document._type?id")
    var documentTypeRef: EntityRef? = null
)
