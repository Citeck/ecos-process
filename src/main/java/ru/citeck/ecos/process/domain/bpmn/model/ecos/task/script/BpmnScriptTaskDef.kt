package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException

data class BpmnScriptTaskDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val resultVariable: String? = null,

    val script: String
) {

    init {
        if (script.isBlank()) throw EcosBpmnDefinitionException("Script task cannot be blank on script task $id")
    }

}
