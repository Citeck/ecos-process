package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.camunda.bpm.engine.impl.bpmn.listener.ScriptExecutionListener
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.scripting.SourceExecutableScript
import org.camunda.bpm.engine.impl.util.xml.Element
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.xml.sax.helpers.AttributesImpl
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_AGENT_REF
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_POSTPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_PREPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TASK_TYPE
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_SET_STATUS
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import javax.xml.namespace.QName

/**
 * The listener reads the deployed Camunda schema, so the elements here are built the way the SAX
 * parser builds them — namespaced attributes, not a stubbed lookup. That keeps the test honest
 * about the attribute key the listener asks for.
 */
class AiTaskScriptsParseListenerTest {

    private val listener = AiTaskParseListener(Mockito.mock(ProcUtils::class.java))

    @Test
    fun `ai task gets all three script listeners`() {
        val activity = parse(
            taskElement(
                BPMN_PROP_ECOS_TASK_TYPE to ECOS_TASK_AI,
                BPMN_PROP_AI_PREPROCESSING_SCRIPT to "var before = 1;",
                BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT to "aiSummary",
                BPMN_PROP_AI_POSTPROCESSING_SCRIPT to "var after = 2;"
            )
        )

        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_START))
            .containsExactly("var before = 1;")

        // the result must be written to the document before the postprocessing script runs,
        // otherwise the script cannot rely on the saved attribute
        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_END))
            .hasSize(2)
            .satisfies({ scripts ->
                assertThat(scripts[0]).contains("document.att(\"aiSummary\", aiResponse)")
                assertThat(scripts[1]).isEqualTo("var after = 2;")
            })
    }

    // A chosen agent only changes which external task handler runs the request; the scripts are a
    // property of the editor element and must keep working either way.
    @Test
    fun `ai task with an agent gets the same script listeners`() {
        val activity = parse(
            taskElement(
                BPMN_PROP_ECOS_TASK_TYPE to ECOS_TASK_AI,
                BPMN_PROP_AI_AGENT_REF to "emodel/ai-agent@tasks-documents-helper",
                BPMN_PROP_AI_PREPROCESSING_SCRIPT to "var before = 1;",
                BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT to "aiSummary",
                BPMN_PROP_AI_POSTPROCESSING_SCRIPT to "var after = 2;"
            )
        )

        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_START)).hasSize(1)
        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_END)).hasSize(2)
    }

    @Test
    fun `ai task without scripts gets no listeners`() {
        val activity = parse(taskElement(BPMN_PROP_ECOS_TASK_TYPE to ECOS_TASK_AI))

        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_START)).isEmpty()
        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_END)).isEmpty()
    }

    @Test
    fun `another ecos task type is left alone`() {
        val activity = parse(
            taskElement(
                BPMN_PROP_ECOS_TASK_TYPE to ECOS_TASK_SET_STATUS,
                BPMN_PROP_AI_PREPROCESSING_SCRIPT to "var before = 1;"
            )
        )

        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_START)).isEmpty()
    }

    @Test
    fun `task without ecos task type is left alone`() {
        val activity = parse(taskElement(BPMN_PROP_AI_PREPROCESSING_SCRIPT to "var before = 1;"))

        assertThat(activity.scriptSources(ExecutionListener.EVENTNAME_START)).isEmpty()
    }

    private fun parse(taskElement: Element): ActivityImpl {
        val processDefinition = ProcessDefinitionEntity().apply { key = "testProcess" }
        val activity = ActivityImpl("testTask", processDefinition)

        listener.parseServiceTask(taskElement, processDefinition, activity)

        return activity
    }

    private fun ActivityImpl.scriptSources(eventName: String): List<String> {
        return getBuiltInListeners(eventName).map {
            // every script must go through the run-as wrapper, see AiTaskScriptsRunAsTest
            ((it as AiTaskParseListener.RunAsWsSystemListener).impl as ScriptExecutionListener).script.let { script ->
                (script as SourceExecutableScript).scriptSource
            }
        }
    }

    private fun taskElement(vararg attributes: Pair<QName, String>): Element {
        val saxAttributes = AttributesImpl()
        attributes.forEach { (name, value) ->
            saxAttributes.addAttribute(name.namespaceURI, name.localPart, "ecos:${name.localPart}", "CDATA", value)
        }
        return Element(BPMN_NS_URI, "serviceTask", "bpmn:serviceTask", saxAttributes, null)
    }

    private companion object {
        const val BPMN_NS_URI = "http://www.omg.org/spec/BPMN/20100524/MODEL"
    }
}
