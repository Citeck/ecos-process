package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import ecos.com.fasterxml.jackson210.annotation.JsonSubTypes
import ecos.com.fasterxml.jackson210.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BpmnSetStatusTaskDef::class, name = ECOS_TASK_SET_STATUS)
)
abstract class BpmnAbstractEcosTaskDef
