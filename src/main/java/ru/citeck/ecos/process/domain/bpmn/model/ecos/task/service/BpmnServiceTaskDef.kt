package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.service

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnServiceTaskDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val type: ServiceTaskType,

    val externalTaskTopic: String? = null,
    val expression: String = "",
    val resultVariable: String = "",

    val multiInstanceConfig: MultiInstanceConfig? = null
) {

    init {
        if (type == ServiceTaskType.EXTERNAL && externalTaskTopic.isNullOrBlank()) {
            throw EcosBpmnElementDefinitionException(
                id,
                "External task topic cannot be blank on external task"
            )
        }

        if (type == ServiceTaskType.EXPRESSION && expression.isBlank()) {
            throw EcosBpmnElementDefinitionException(
                id,
                "Expression cannot be blank on expression task"
            )
        }
    }
}

enum class ServiceTaskType {
    EXTERNAL, EXPRESSION
}
