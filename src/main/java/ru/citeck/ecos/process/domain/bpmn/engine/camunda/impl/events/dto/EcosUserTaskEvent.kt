package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant

data class EcosUserTaskEvent(
    val record: EntityRef,
    val taskRef: EntityRef,
    val engine: String,
    val form: EntityRef,
    val procDefId: String,
    val procDefRef: EntityRef,
    val procDeploymentVersion: Int?,
    val procInstanceId: EntityRef,
    val processId: String,
    val processRef: EntityRef,
    val elementDefId: String,
    val created: Instant?,
    var assignee: String? = null,
    var assigneeRef: EntityRef? = null,
    var candidateGroups: List<String> = emptyList(),
    var candidateGroupsRef: List<EntityRef> = emptyList(),
    var candidateUsers: List<String> = emptyList(),
    var candidateUsersRef: List<EntityRef> = emptyList()
)
