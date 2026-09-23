package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

const val ECOS_TASK_AI = "aiTask"

@JsonTypeName(ECOS_TASK_AI)
data class BpmnAiTaskDef(
    val userInput: String,

    val preProcessedScript: String,
    val postProcessedScript: String,

    val addDocumentToContext: Boolean,
    val saveResultToDocumentAtt: String,

    /**
     * Optional. Empty means a plain AI request; a filled ref routes the task to the AI agent
     * handler instead, which runs the agent's own model, instruction and tools.
     * Defaults to empty because process definitions stored before this field existed do not
     * carry it — see CamundaTaskConverter for how the ref picks the external task topic.
     */
    val agentRef: String = ""
) : BpmnAbstractEcosTaskDef(),
    Validated {

    override fun validate() {
        if (userInput.isBlank()) {
            throw EcosBpmnDefinitionException("On AI task def, user input cannot be blank. $this")
        }
    }
}
