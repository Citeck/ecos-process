package ru.citeck.ecos.process.domain.bpmn.io.convert

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_AGENT_REF
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TASK_TYPE
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnAiTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnSetStatusTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTask

/**
 * Guards the ecos-format round-trip of [BpmnAbstractEcosTaskDef] subtypes through
 * [fillEcosTaskDefToOtherAttributes] (export) and [convertToBpmnEcosTaskDef] (import).
 *
 * Both functions are `when` chains ending in `error("Unsupported task type: ...")`, and both are
 * called by `BpmnTaskConverter` on every read/write of the editor format. A task type registered
 * everywhere else (model, Camunda converter, parse listener, editor form) but missing here fails
 * at process save/load with that error — which is exactly how `aiAgentTask` shipped initially,
 * before it was folded into the AI task as an optional agent ref.
 */
class BpmnEcosTaskDefConvertationsTest {

    @Test
    fun `ai task with an agent should survive the ecos format round-trip`() {
        val source = BpmnAiTaskDef(
            userInput = "Summarize \${documentTitle}",
            preProcessedScript = "var a = 1;",
            postProcessedScript = "var b = 2;",
            addDocumentToContext = false,
            saveResultToDocumentAtt = "aiSummary",
            agentRef = "emodel/ai-agent@tasks-documents-helper"
        )

        val exported = TTask().apply { fillEcosTaskDefToOtherAttributes(source) }

        // one task type either way — the agent is a property of the task, not a type of its own
        assertThat(exported.otherAttributes[BPMN_PROP_ECOS_TASK_TYPE]).isEqualTo("aiTask")
        assertThat(exported.convertToBpmnEcosTaskDef()).isEqualTo(source)
    }

    // Definitions stored before the agent field existed carry no such attribute at all.
    @Test
    fun `ai task without an agent attribute should read as having no agent`() {
        val source = BpmnAiTaskDef(
            userInput = "Do the thing",
            preProcessedScript = "",
            postProcessedScript = "",
            addDocumentToContext = true,
            saveResultToDocumentAtt = ""
        )

        val exported = TTask().apply { fillEcosTaskDefToOtherAttributes(source) }
        exported.otherAttributes.remove(BPMN_PROP_AI_AGENT_REF)

        val imported = exported.convertToBpmnEcosTaskDef()

        assertThat(imported).isEqualTo(source)
        assertThat((imported as BpmnAiTaskDef).agentRef).isEmpty()
    }

    @Test
    fun `ai task should survive the ecos format round-trip`() {
        val source = BpmnAiTaskDef(
            userInput = "Explain the document",
            preProcessedScript = "var a = 1;",
            postProcessedScript = "var b = 2;",
            addDocumentToContext = true,
            saveResultToDocumentAtt = "aiResult"
        )

        val exported = TTask().apply { fillEcosTaskDefToOtherAttributes(source) }

        assertThat(exported.otherAttributes[BPMN_PROP_ECOS_TASK_TYPE]).isEqualTo("aiTask")
        assertThat(exported.convertToBpmnEcosTaskDef()).isEqualTo(source)
    }

    @Test
    fun `set status task should survive the ecos format round-trip`() {
        val source = BpmnSetStatusTaskDef(status = "approved")

        val exported = TTask().apply { fillEcosTaskDefToOtherAttributes(source) }

        assertThat(exported.convertToBpmnEcosTaskDef()).isEqualTo(source)
    }

    @Test
    fun `task without ecos task type should convert to null`() {
        assertThat(TTask().convertToBpmnEcosTaskDef()).isNull()

        val blank = TTask().apply { otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] = "" }
        assertThat(blank.convertToBpmnEcosTaskDef()).isNull()
    }

    @Test
    fun `unknown ecos task type should still fail loud`() {
        val unknown = TTask().apply { otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] = "someFutureTask" }

        assertThatThrownBy { unknown.convertToBpmnEcosTaskDef() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unsupported task type: someFutureTask")
    }
}
