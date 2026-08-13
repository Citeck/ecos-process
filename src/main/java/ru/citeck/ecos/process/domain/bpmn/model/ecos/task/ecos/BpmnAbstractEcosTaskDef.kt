package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BpmnSetStatusTaskDef::class, name = ECOS_TASK_SET_STATUS),
    JsonSubTypes.Type(value = BpmnAiTaskDef::class, name = ECOS_TASK_AI)
)
abstract class BpmnAbstractEcosTaskDef
