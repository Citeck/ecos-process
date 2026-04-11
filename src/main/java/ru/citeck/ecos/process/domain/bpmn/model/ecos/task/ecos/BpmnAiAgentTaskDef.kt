package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos

import com.fasterxml.jackson.annotation.JsonTypeName
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

const val ECOS_TASK_AI_AGENT = "aiAgentTask"

@JsonTypeName(ECOS_TASK_AI_AGENT)
data class BpmnAiAgentTaskDef(
    val agentRef: String,
    val userInput: String,

    val preProcessedScript: String,
    val postProcessedScript: String,

    val addDocumentToContext: Boolean,
    val saveResultToDocumentAtt: String
) : BpmnAbstractEcosTaskDef(),
    Validated {

    override fun validate() {
        if (agentRef.isBlank()) {
            throw EcosBpmnDefinitionException("On AI Agent task def, agentRef cannot be blank. $this")
        }
        if (userInput.isBlank()) {
            throw EcosBpmnDefinitionException("On AI Agent task def, user input cannot be blank. $this")
        }
    }
}
