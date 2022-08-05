package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig

data class BpmnScriptTaskDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val resultVariable: String? = null,

    val script: String,

    val multiInstanceConfig: MultiInstanceConfig? = null
) {

    init {
        if (script.isBlank()) throw EcosBpmnDefinitionException("Script task cannot be blank on script task $id")
    }
}
