package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessRecordsActivityTreeTest {

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var camundaRuntimeService: RuntimeService

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private const val USER_TASK_PROCESS_ID = "test-user-task-assign-manual"
        private const val MI_PROCESS_ID = "test-sub-process-multi-instance-parallel"
        private const val MI_SUBPROCESS_ACTIVITY_ID = "Activity_0ms00bo"
    }

    @BeforeEach
    fun setUp() {
        helper.saveAndDeployBpmn("usertask", USER_TASK_PROCESS_ID)
        helper.saveAndDeployBpmn("subprocess", MI_PROCESS_ID)
    }

    @Test
    fun `activityInstanceTree returns null for completed process instance`() {
        val pid = startUserTaskProcess()

        bpmnProcessService.deleteProcessInstance(pid, "test cleanup")

        val tree = recordsService.getAtt(procRef(pid), "activityInstanceTree?json")
        assertThat(tree.isNull()).isTrue()
    }

    @Test
    fun `activityInstanceTree returns root with user task child for active process`() {
        val pid = startUserTaskProcess()

        val tree = recordsService.getAtt(procRef(pid), "activityInstanceTree?json")

        assertThat(tree.isNotNull()).isTrue()
        assertThat(tree.get("activityType").asText()).isEqualTo("processDefinition")
        assertThat(tree.get("processInstanceId").asText()).isEqualTo(pid)
        assertThat(tree.get("activityId").asText()).isNotBlank()

        val children = tree.get("childActivityInstances")
        assertThat(children.size()).isGreaterThan(0)

        val userTaskNode = children.firstOrNull { it.get("activityId").asText() == "userTask" }
        assertThat(userTaskNode)
            .withFailMessage { "userTask child not found in tree: $tree" }
            .isNotNull
        assertThat(userTaskNode!!.get("activityType").asText()).isEqualTo("userTask")
        assertThat(userTaskNode.get("processInstanceId").asText()).isEqualTo(pid)
    }

    @Test
    fun `activityInstanceTree exposes multiInstance body with inner instances`() {
        val pid = camundaRuntimeService.startProcessInstanceByKey(
            MI_PROCESS_ID,
            mapOf("testCandidateCollection" to listOf("alice", "bob", "carol"))
        ).processInstanceId

        val tree = recordsService.getAtt(procRef(pid), "activityInstanceTree?json")

        val children = tree.get("childActivityInstances")
        val miBody = children.firstOrNull { it.get("activityType").asText() == "multiInstanceBody" }
        assertThat(miBody)
            .withFailMessage { "multiInstanceBody node not found in tree: $tree" }
            .isNotNull
        assertThat(miBody!!.get("activityId").asText())
            .isEqualTo("$MI_SUBPROCESS_ACTIVITY_ID#multiInstanceBody")

        val innerInstances = miBody.get("childActivityInstances").toList()
        assertThat(innerInstances).hasSize(3)
        assertThat(innerInstances.map { it.get("activityType").asText() }.toSet())
            .containsExactly("subProcess")
        assertThat(innerInstances.map { it.get("activityId").asText() }.toSet())
            .containsExactly(MI_SUBPROCESS_ACTIVITY_ID)

        // Each inner subProcess instance carries its own child (userTask).
        innerInstances.forEach { inner ->
            assertThat(inner.get("childActivityInstances").size())
                .withFailMessage { "inner subProcess has no children: $inner" }
                .isGreaterThan(0)
        }
    }

    @Test
    fun `activityInstanceTree returns null for unknown process instance id`() {
        val tree = recordsService.getAtt(procRef("unknown-process-instance"), "activityInstanceTree?json")
        assertThat(tree.isNull()).isTrue()
    }

    @Test
    fun `activityInstanceTree returns null when caller has no PROC_INSTANCE_READ permission`() {
        val pid = startUserTaskProcess()

        val tree = AuthContext.runAs("userWithoutPerms") {
            recordsService.getAtt(procRef(pid), "activityInstanceTree?json")
        }
        assertThat(tree.isNull()).isTrue()
    }

    private fun startUserTaskProcess(): String {
        return camundaRuntimeService.startProcessInstanceByKey(USER_TASK_PROCESS_ID).processInstanceId
    }

    private fun procRef(pid: String): EntityRef {
        return EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, pid)
    }
}
