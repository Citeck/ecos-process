package ru.citeck.ecos.process.domain.bpmn.elements.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.time.Instant

data class TaskElementEvent(
    var taskId: String? = null,
    var engine: String? = null,
    var assignee: String? = null,
    var procDefId: String? = null,
    var procDeploymentVersion: Int? = null,
    var procInstanceId: String? = null,
    var elementDefId: String? = null,
    var created: Instant? = null,
    var dueDate: Instant? = null,
    var description: String? = null,
    var priority: Int? = null,
    var executionId: String? = null,
    var name: MLText? = null,
    var comment: String? = null,
    var outcome: String? = null,
    var outcomeName: MLText? = null,
    var document: RecordRef? = null,

    @AttName("document._type?id")
    var documentTypeRef: RecordRef? = null,

    @AttName("\$event.time")
    var time: String? = null
)
