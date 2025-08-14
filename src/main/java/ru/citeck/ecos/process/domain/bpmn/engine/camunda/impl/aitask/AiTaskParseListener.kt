package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask

import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.impl.bpmn.listener.ScriptExecutionListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.scripting.SourceExecutableScript
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_POSTPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_PREPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TASK_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.convert.toCamundaKey
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI

@Component
class AiTaskParseListener() : AbstractBpmnParseListener() {

    companion object {
        private const val AI_TASK_SCRIPT_LANGUAGE = "javascript"
        private const val AI_RESPONSE_ATT = "aiResponse"
    }

    override fun parseServiceTask(
        taskElement: Element,
        scope: ScopeImpl,
        activity: ActivityImpl
    ) {
        if (taskElement.isAiTask().not()) {
            return
        }

        val preprocessedScript = taskElement.attribute(BPMN_PROP_AI_PREPROCESSING_SCRIPT.toCamundaKey()) ?: ""
        if (preprocessedScript.isNotBlank()) {
            val scriptListener = ScriptExecutionListener(
                SourceExecutableScript(AI_TASK_SCRIPT_LANGUAGE, preprocessedScript)
            )

            activity.addBuiltInListener(ExecutionListener.EVENTNAME_START, scriptListener)
        }

        val saveAiResultToDocumentAtt = taskElement.attribute(
            BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT.toCamundaKey()
        ) ?: ""
        if (saveAiResultToDocumentAtt.isNotBlank()) {
            val scriptListener = ScriptExecutionListener(
                SourceExecutableScript(
                    AI_TASK_SCRIPT_LANGUAGE,
                    """
                        document.att("$saveAiResultToDocumentAtt", $AI_RESPONSE_ATT);
                        document.save();
                    """.trimIndent()
                )
            )
            activity.addBuiltInListener(ExecutionListener.EVENTNAME_END, scriptListener, 0)
        }

        val postProcessedScript = taskElement.attribute(BPMN_PROP_AI_POSTPROCESSING_SCRIPT.toCamundaKey()) ?: ""
        if (postProcessedScript.isNotBlank()) {
            val scriptListener = ScriptExecutionListener(
                SourceExecutableScript(AI_TASK_SCRIPT_LANGUAGE, postProcessedScript)
            )

            activity.addBuiltInListener(ExecutionListener.EVENTNAME_END, scriptListener, 1)
        }
    }

    private fun Element.isAiTask(): Boolean {
        val taskType = this.attribute(BPMN_PROP_ECOS_TASK_TYPE.toCamundaKey())
        return taskType == ECOS_TASK_AI
    }
}
