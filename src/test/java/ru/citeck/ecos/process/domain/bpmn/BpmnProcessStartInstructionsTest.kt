package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.START_INSTRUCTION_START_AFTER_ACTIVITY
import ru.citeck.ecos.process.domain.bpmn.process.START_INSTRUCTION_START_BEFORE_ACTIVITY
import ru.citeck.ecos.process.domain.bpmn.process.StartInstruction
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessStartInstructionsTest {

    companion object {
        private const val PROCESS_ID = "test-sub-process-start-instructions"

        private const val SCRIPT_TASK_BEFORE_ID = "scriptTaskBefore"
        private const val SUB_PROCESS_ID = "subProcess"
        private const val SUB_USER_TASK_ID = "subUserTask"

        private const val VAR_BEFORE_NODE_EXECUTED = "beforeNodeExecuted"
        private const val VAR_SUB_PROCESS_ENTERED = "subProcessEntered"

        private const val ATT_START_INSTRUCTIONS = "startInstructions"
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var camundaRuntimeService: RuntimeService

    @Autowired
    private lateinit var camundaTaskService: TaskService

    @Autowired
    private lateinit var camundaHistoryService: HistoryService

    @BeforeEach
    fun setUp() {
        helper.saveAndDeployBpmn("subprocess", PROCESS_ID)
    }

    @AfterEach
    fun tearDown() {
        helper.cleanDeployments()
        helper.cleanDefinitions()
    }

    @Test
    fun `start with startBeforeActivity on subprocess should skip nodes before subprocess`() {
        val processInstanceId = startProcess(
            startInstructions = listOf(
                instruction(START_INSTRUCTION_START_BEFORE_ACTIVITY, SUB_PROCESS_ID)
            )
        )

        val activeTasks = camundaTaskService.createTaskQuery().processInstanceId(processInstanceId).list()
        assertThat(activeTasks).hasSize(1)
        assertThat(activeTasks[0].taskDefinitionKey).isEqualTo(SUB_USER_TASK_ID)

        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_SUB_PROCESS_ENTERED)).isEqualTo(true)
        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_BEFORE_NODE_EXECUTED)).isNull()
    }

    @Test
    fun `start with startAfterActivity should continue from the next node without executing it`() {
        val processInstanceId = startProcess(
            startInstructions = listOf(
                instruction(START_INSTRUCTION_START_AFTER_ACTIVITY, SCRIPT_TASK_BEFORE_ID)
            )
        )

        val activeTasks = camundaTaskService.createTaskQuery().processInstanceId(processInstanceId).list()
        assertThat(activeTasks).hasSize(1)
        assertThat(activeTasks[0].taskDefinitionKey).isEqualTo(SUB_USER_TASK_ID)

        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_SUB_PROCESS_ENTERED)).isEqualTo(true)
        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_BEFORE_NODE_EXECUTED)).isNull()
    }

    @Test
    fun `start instruction variables should be local to the created activity instance`() {
        // Two instructions on the same subprocess: the same variable name with different values
        // can only coexist if each value is local to its own activity instance. Note: the exact
        // execution the variable lands on is engine business — with a compacted execution tree
        // Camunda may place the first instruction's variables on the process instance execution.
        val processInstanceId = startProcess(
            startInstructions = listOf(
                instruction(
                    START_INSTRUCTION_START_BEFORE_ACTIVITY,
                    SUB_PROCESS_ID,
                    variables = mapOf("localVar" to "firstValue")
                ),
                instruction(
                    START_INSTRUCTION_START_BEFORE_ACTIVITY,
                    SUB_PROCESS_ID,
                    variables = mapOf("localVar" to "secondValue")
                )
            )
        )

        val tasks = camundaTaskService.createTaskQuery().processInstanceId(processInstanceId).list()
        assertThat(tasks).hasSize(2)

        val localValues = tasks.map { camundaTaskService.getVariable(it.id, "localVar") }
        assertThat(localValues).containsExactlyInAnyOrder("firstValue", "secondValue")
    }

    @Test
    fun `start instructions attribute should not be stored as process variable`() {
        val processInstanceId = startProcess(
            variables = mapOf("customVar" to "customValue"),
            startInstructions = listOf(
                instruction(START_INSTRUCTION_START_BEFORE_ACTIVITY, SUB_PROCESS_ID)
            )
        )

        val processVariables = camundaRuntimeService.getVariables(processInstanceId)
        assertThat(processVariables).doesNotContainKey(ATT_START_INSTRUCTIONS)
        assertThat(processVariables["customVar"]).isEqualTo("customValue")
    }

    @Test
    fun `start without instructions should start from default start event`() {
        val processInstanceId = startProcess()

        val activeTasks = camundaTaskService.createTaskQuery().processInstanceId(processInstanceId).list()
        assertThat(activeTasks).hasSize(1)
        assertThat(activeTasks[0].taskDefinitionKey).isEqualTo(SUB_USER_TASK_ID)

        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_BEFORE_NODE_EXECUTED)).isEqualTo(true)
        assertThat(camundaRuntimeService.getVariable(processInstanceId, VAR_SUB_PROCESS_ENTERED)).isEqualTo(true)
    }

    @Test
    fun `start via service with start instructions should keep business key`() {
        val businessKey = "start-instructions-business-key"

        val instance = bpmnProcessService.startProcess(
            StartProcessRequest(
                workspace = "",
                processId = PROCESS_ID,
                businessKey = businessKey,
                startInstructions = listOf(
                    StartInstruction(START_INSTRUCTION_START_BEFORE_ACTIVITY, SUB_PROCESS_ID)
                )
            )
        )

        assertThat(instance.businessKey).isEqualTo(businessKey)
        assertThat(camundaRuntimeService.getVariable(instance.id, VAR_BEFORE_NODE_EXECUTED)).isNull()

        val task = camundaTaskService.createTaskQuery().processInstanceId(instance.id).singleResult()
        assertThat(task.taskDefinitionKey).isEqualTo(SUB_USER_TASK_ID)
    }

    @Test
    fun `start with unknown instruction type should fail without instance creation`() {
        assertThrows<IllegalArgumentException> {
            startProcess(
                startInstructions = listOf(
                    instruction("startUnknownInstruction", SUB_PROCESS_ID)
                )
            )
        }

        assertNoProcessInstancesStarted()
    }

    @Test
    fun `start with blank activity id should fail without instance creation`() {
        assertThrows<IllegalArgumentException> {
            startProcess(
                startInstructions = listOf(
                    instruction(START_INSTRUCTION_START_BEFORE_ACTIVITY, "")
                )
            )
        }

        assertNoProcessInstancesStarted()
    }

    @Test
    fun `start with not existing activity id should fail without instance creation`() {
        assertThrows<Exception> {
            startProcess(
                startInstructions = listOf(
                    instruction(START_INSTRUCTION_START_BEFORE_ACTIVITY, "notExistingActivityId")
                )
            )
        }

        assertNoProcessInstancesStarted()
    }

    private fun instruction(type: String, activityId: String, variables: Map<String, Any> = emptyMap()): Map<String, Any> {
        val result = mutableMapOf<String, Any>(
            "type" to type,
            "activityId" to activityId
        )
        if (variables.isNotEmpty()) {
            result["variables"] = variables
        }
        return result
    }

    private fun startProcess(
        variables: Map<String, Any> = emptyMap(),
        startInstructions: List<Map<String, Any>> = emptyList()
    ): String {
        val startAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
            variables.forEach { (key, value) -> this[key] = value }
            if (startInstructions.isNotEmpty()) {
                this[ATT_START_INSTRUCTIONS] = DataValue.create(startInstructions)
            }
        }

        return recordsService.mutate(startAtts).getLocalId()
    }

    private fun assertNoProcessInstancesStarted() {
        assertThat(
            camundaRuntimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_ID)
                .count()
        ).isZero()
        assertThat(
            camundaHistoryService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_ID)
                .count()
        ).isZero()
    }
}
