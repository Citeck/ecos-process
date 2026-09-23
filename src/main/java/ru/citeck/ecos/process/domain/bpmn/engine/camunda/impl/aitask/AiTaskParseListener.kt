package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.impl.bpmn.listener.ScriptExecutionListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
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
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils

/**
 * Attaches the preprocessing / result saving / postprocessing script listeners to an AI task.
 * Applies to every AI task, agent-backed or not: the scripts are a property of the editor
 * element, and the chosen agent only decides which external task handler runs the request.
 *
 * The scripts are user code from the process definition, so they run under the same identity as a
 * script task (see ScriptTaskParseListener): the workspace-system user for a workspace process and
 * system for a global one. Without it they would inherit whatever context reaches the task — the
 * user who moved the process for the start script, and full system for the end scripts, because
 * citeck-ai completes the external task with a system token.
 *
 * Must stay registered in EcosCamundaParseListenerPlugin — a listener that is only annotated
 * with @Component never reaches the engine.
 */
@Component
class AiTaskParseListener(
    private val procUtils: ProcUtils
) : AbstractBpmnParseListener() {

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

        // The key carries the workspace system id prefix — the same source jobs and task events use.
        val procDefKey = (activity.processDefinition as ProcessDefinitionEntity).key
        fun scriptListener(script: String): ExecutionListener = RunAsWsSystemListener(
            procUtils,
            procDefKey,
            ScriptExecutionListener(SourceExecutableScript(AI_TASK_SCRIPT_LANGUAGE, script))
        )

        val preprocessedScript = taskElement.attribute(BPMN_PROP_AI_PREPROCESSING_SCRIPT.toCamundaKey()) ?: ""
        if (preprocessedScript.isNotBlank()) {
            activity.addBuiltInListener(ExecutionListener.EVENTNAME_START, scriptListener(preprocessedScript))
        }

        val saveAiResultToDocumentAtt = taskElement.attribute(
            BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT.toCamundaKey()
        ) ?: ""
        if (saveAiResultToDocumentAtt.isNotBlank()) {
            val saveResultScript = """
                document.att("$saveAiResultToDocumentAtt", $AI_RESPONSE_ATT);
                document.save();
            """.trimIndent()
            activity.addBuiltInListener(ExecutionListener.EVENTNAME_END, scriptListener(saveResultScript), 0)
        }

        val postProcessedScript = taskElement.attribute(BPMN_PROP_AI_POSTPROCESSING_SCRIPT.toCamundaKey()) ?: ""
        if (postProcessedScript.isNotBlank()) {
            activity.addBuiltInListener(ExecutionListener.EVENTNAME_END, scriptListener(postProcessedScript), 1)
        }
    }

    private fun Element.isAiTask(): Boolean {
        val taskType = this.attribute(BPMN_PROP_ECOS_TASK_TYPE.toCamundaKey())
        return taskType == ECOS_TASK_AI
    }

    class RunAsWsSystemListener(
        private val procUtils: ProcUtils,
        private val procDefKey: String,
        val impl: ExecutionListener
    ) : ExecutionListener {

        override fun notify(execution: DelegateExecution) {
            procUtils.runAsWsSystemIfRequiredForProcDef(procDefKey) {
                impl.notify(execution)
            }
        }
    }
}
