package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.aitask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.ExternalTaskService
import org.camunda.bpm.engine.RuntimeService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthUser
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The AI task scripts are user code from the process definition, just like a script task, so they
 * must run under the same identity: the workspace-system user for a workspace process and the
 * system user for a global one. The external task is completed here as system on purpose — that
 * is how citeck-ai completes it over REST, and it is exactly the context the end scripts would
 * otherwise inherit.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class AiTaskScriptsRunAsTest {

    companion object {
        private const val PROC_ID = "test-ai-task-run-as"
        private const val WORKSPACE = "ai-task-ws"
        private const val AI_TASK_TOPIC = "citeck-bpmn-ai-task"
        private const val WORKER_ID = "ai-task-run-as-test-worker"
        private const val PROBE_SOURCE_ID = "ai-task-run-as-probe"
        private const val INITIATOR = "ivan"
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var workspaceService: WorkspaceService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var externalTaskService: ExternalTaskService

    private val probe = ProbeRecordsDao()

    @BeforeEach
    fun setUp() {
        recordsService.register(probe)
    }

    @AfterEach
    fun tearDown() {
        recordsService.unregister(PROBE_SOURCE_ID)
        helper.cleanDefinitions()
        helper.cleanDeployments()
    }

    @Test
    fun `ai task scripts of a workspace process run as the workspace system user`() {
        runProcess(WORKSPACE)

        val wsSystemUser = "ws_system_" + workspaceService.getWorkspaceSystemId(WORKSPACE)
        assertThat(probe.writes).containsExactly(
            "pre" to wsSystemUser,
            "ai answer" to wsSystemUser,
            "post" to wsSystemUser
        )
    }

    @Test
    fun `ai task scripts of a global process run as system`() {
        runProcess("")

        assertThat(probe.writes).containsExactly(
            "pre" to AuthUser.SYSTEM,
            "ai answer" to AuthUser.SYSTEM,
            "post" to AuthUser.SYSTEM
        )
    }

    private fun runProcess(workspace: String) {
        val definition = ResourceUtils.getFile("classpath:test/bpmn/elements/aitask/$PROC_ID.bpmn.xml").readText()
        helper.saveAndDeployBpmnFromString(definition, PROC_ID, workspace)

        val documentRef = EntityRef.create(PROBE_SOURCE_ID, "doc")
        val process = AuthContext.runAs(INITIATOR) {
            bpmnProcessService.startProcess(
                StartProcessRequest(
                    workspace = workspace,
                    processId = PROC_ID,
                    businessKey = documentRef.toString(),
                    variables = mapOf(BPMN_DOCUMENT_REF to documentRef.toString())
                )
            )
        }

        val task = externalTaskService.fetchAndLock(10, WORKER_ID)
            .topic(AI_TASK_TOPIC, 60_000)
            .processDefinitionId(process.processDefinitionId)
            .execute()
            .single { it.processInstanceId == process.id }

        AuthContext.runAsSystem {
            externalTaskService.complete(task.id, WORKER_ID, mapOf("aiResponse" to "ai answer"))
        }

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(process.id).count())
            .describedAs("process must run to the end")
            .isZero()
    }

    private class ProbeRecordsDao :
        AbstractRecordsDao(),
        RecordAttsDao,
        RecordMutateDao {

        val writes = CopyOnWriteArrayList<Pair<String, String>>()

        override fun getId() = PROBE_SOURCE_ID

        override fun getRecordAtts(recordId: String): Any = ProbeRecord(recordId)

        override fun mutate(record: LocalRecordAtts): String {
            writes.add(record.getAtt("probe").asText() to AuthContext.getCurrentRunAsUser())
            return record.id
        }
    }

    private class ProbeRecord(val id: String)
}
