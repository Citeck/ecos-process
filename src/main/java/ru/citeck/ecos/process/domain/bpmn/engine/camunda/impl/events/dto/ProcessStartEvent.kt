package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto

import ru.citeck.ecos.webapp.api.entity.EntityRef

data class ProcessStartEvent(
    val processKey: String,
    val processInstanceId: String,
    val processDefinitionId: String,
    val document: EntityRef
)
