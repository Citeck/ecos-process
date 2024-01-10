package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

data class UserTaskEvent(
    var taskId: EntityRef? = null,
    var engine: String? = null,
    var form: EntityRef? = null,
    var assignee: String? = null,
    var procDefId: String? = null,
    var procDefRef: EntityRef? = null,
    var procDeploymentVersion: Int? = null,
    var procInstanceId: EntityRef? = null,
    var processId: String? = null,
    var processRef: EntityRef? = null,
    var elementDefId: String? = null,
    var created: Instant? = null,
    var dueDate: Instant? = null,
    var description: String? = null,
    var priority: Int? = null,
    var executionId: String? = null,
    var name: MLText? = null,
    var comment: String? = null,
    var completedBy: String? = null,
    var outcome: String? = null,
    var outcomeName: MLText? = null,
    var completedOnBehalfOf: String? = null,
    var document: EntityRef? = null,
    var roles: List<TaskRole> = emptyList(),

    @AttName("document._type?id")
    var documentTypeRef: EntityRef? = null,

    @AttName("\$event.time")
    var time: String? = null
)

data class TaskRole(
    val id: String,
    val name: MLText
)
