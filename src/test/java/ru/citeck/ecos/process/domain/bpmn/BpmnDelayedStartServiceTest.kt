package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.Awaitility
import org.camunda.bpm.engine.RuntimeService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.camunda.bpm.engine.RuntimeService
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.bpmn.process.delayed.BpmnDelayedStartCmdDesc.BPMN_DELAYED_START_CMD_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.process.delayed.BpmnDelayedStartService
import ru.citeck.ecos.process.domain.bpmn.process.delayed.BpmnDelayedStartService.Companion.getDelay
import ru.citeck.ecos.process.domain.bpmn.process.delayed.BpmnDelayedStartService.Companion.parseDelays
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnDelayedStartServiceTest {

    companion object {
        private const val PROC_ID = "bpmn-start-async-test"
        private const val NON_EXISTENT_PROC_ID = "non-existent-process-def-for-delayed-start-test"
    }

    @Autowired
    private lateinit var delayedStartService: BpmnDelayedStartService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @BeforeAll
    fun setUp() {
        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    // region parseDelays

    @Test
    fun `parseDelays should parse various duration units`() {
        val delays = parseDelays("5s 10s 30s 1m 5m 1h 1d")
        assertThat(delays).containsExactly(
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            Duration.ofSeconds(30),
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofHours(1),
            Duration.ofDays(1)
        )
    }

    @Test
    fun `parseDelays should handle single value`() {
        val delays = parseDelays("30s")
        assertThat(delays).containsExactly(Duration.ofSeconds(30))
    }

    @Test
    fun `parseDelays should handle extra whitespace`() {
        val delays = parseDelays("  5s   10m  ")
        assertThat(delays).containsExactly(
            Duration.ofSeconds(5),
            Duration.ofMinutes(10)
        )
    }

    @Test
    fun `parseDelays should return empty list for blank string`() {
        assertThat(parseDelays("")).isEmpty()
        assertThat(parseDelays("   ")).isEmpty()
    }

    @Test
    fun `parseDelays should throw on unknown unit`() {
        assertThatThrownBy { parseDelays("5x") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Unknown duration unit")
    }

    // endregion

    // region getDelay

    @Test
    fun `getDelay should return delay by index when retryCount is within configured delays`() {
        val delays = listOf(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5))
        assertThat(getDelay(0, delays, false)).isEqualTo(Duration.ofSeconds(1))
        assertThat(getDelay(1, delays, false)).isEqualTo(Duration.ofSeconds(2))
        assertThat(getDelay(2, delays, false)).isEqualTo(Duration.ofSeconds(5))
    }

    @Test
    fun `getDelay should return last delay when retryCount exceeds list and doubling is off`() {
        val delays = listOf(Duration.ofSeconds(1), Duration.ofSeconds(5))
        assertThat(getDelay(2, delays, false)).isEqualTo(Duration.ofSeconds(5))
        assertThat(getDelay(10, delays, false)).isEqualTo(Duration.ofSeconds(5))
        assertThat(getDelay(100, delays, false)).isEqualTo(Duration.ofSeconds(5))
    }

    @Test
    fun `getDelay should double last delay when doubling is on and retryCount exceeds list`() {
        val delays = listOf(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5))
        // retryCount=3 → 1 doubling: 5*2 = 10s
        assertThat(getDelay(3, delays, true)).isEqualTo(Duration.ofSeconds(10))
        // retryCount=4 → 2 doublings: 5*2*2 = 20s
        assertThat(getDelay(4, delays, true)).isEqualTo(Duration.ofSeconds(20))
        // retryCount=5 → 3 doublings: 5*2*2*2 = 40s
        assertThat(getDelay(5, delays, true)).isEqualTo(Duration.ofSeconds(40))
    }

    @Test
    fun `getDelay should return 1 minute fallback when delays list is empty`() {
        assertThat(getDelay(0, emptyList(), false)).isEqualTo(Duration.ofMinutes(1))
        assertThat(getDelay(5, emptyList(), true)).isEqualTo(Duration.ofMinutes(1))
    }

    @Test
    fun `getDelay should cap doublings at 20`() {
        val delays = listOf(Duration.ofSeconds(1))
        // retryCount=21 → 21 doublings, but capped at 20 → 1 * 2^20 = 1048576s
        val expected = Duration.ofSeconds(1_048_576)

        assertThat(getDelay(21, delays, true)).isEqualTo(expected)
        // retryCount=100 → still capped at 20
        assertThat(getDelay(100, delays, true)).isEqualTo(expected)
    }

    // endregion

    // region saveForDelayedRetry

    @Test
    fun `saveForDelayedRetry should persist command to DB`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = "test-proc-save",
            businessKey = "bk-save-test",
            variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin", "customVar" to "value1")
        )

        delayedStartService.saveForDelayedRetry(request)

        val records = queryDelayedCommands("test-proc-save")
        assertThat(records).hasSize(1)

        val cmd = records.first()
        assertThat(cmd.processId).isEqualTo("test-proc-save")
        assertThat(cmd.businessKey).isEqualTo("bk-save-test")
        assertThat(cmd.workspace).isEqualTo("")
        assertThat(cmd.retryCount).isEqualTo(0)
        assertThat(cmd.nextRetryTime).isAfter(Instant.now().minusSeconds(10))

        deleteDelayedCommand(cmd.ref)
    }

    @Test
    fun `saveForDelayedRetry with completed=true should set completedAt and epoch nextRetryTime`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = "test-proc-completed",
            businessKey = "bk-completed-test",
            variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin")
        )

        val beforeSave = Instant.now()
        delayedStartService.saveForDelayedRetry(request, completed = true)

        val cmds = queryDelayedCommands("test-proc-completed")
        assertThat(cmds).hasSize(1)

        val cmd = cmds.first()
        assertThat(cmd.completedAt).isNotNull()
        assertThat(cmd.completedAt).isAfterOrEqualTo(beforeSave)
        assertThat(cmd.nextRetryTime).isEqualTo(Instant.EPOCH)
        assertThat(cmd.retryCount).isEqualTo(0)

        deleteDelayedCommand(cmd.ref)
    }

    @Test
    fun `saveForDelayedRetry should preserve variables including workflow initiator`() {
        val variables = mapOf<String, Any?>(
            BPMN_WORKFLOW_INITIATOR to "testuser",
            "documentRef" to "doc@123",
            "amount" to 42
        )

        val request = StartProcessRequest(
            workspace = "test-ws",
            processId = "test-proc-vars",
            businessKey = "bk-vars-test",
            variables = variables
        )

        delayedStartService.saveForDelayedRetry(request)

        val cmds = queryDelayedCommands("test-proc-vars")
        assertThat(cmds).hasSize(1)

        val savedVars = cmds.first().variables
        assertThat(savedVars.get(BPMN_WORKFLOW_INITIATOR).asText()).isEqualTo("testuser")
        assertThat(savedVars.get("documentRef").asText()).isEqualTo("doc@123")
        assertThat(savedVars.get("amount").asInt()).isEqualTo(42)

        deleteDelayedCommand(cmds.first().ref)
    }

    // endregion

    // region startProcessWithAuth

    @Test
    fun `startProcessWithAuth should start process as system when no initiator`() {
        val businessKey = "auth-system-test-${System.currentTimeMillis()}"
        val request = StartProcessRequest(
            workspace = "",
            processId = PROC_ID,
            businessKey = businessKey,
            variables = emptyMap()
        )

        delayedStartService.startProcessWithAuth(request)

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val processes = bpmnProcessService.getProcessInstancesForBusinessKey(businessKey)
            assertThat(processes).hasSize(1)
        }
    }

    // endregion

    // region processDelayedCommands

    @Test
    fun `processDelayedCommands should start process and mark record as completed on success`() {
        val businessKey = "delayed-start-success-${System.currentTimeMillis()}"

        val request = StartProcessRequest(
            workspace = "",
            processId = PROC_ID,
            businessKey = businessKey,
            variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin")
        )

        saveDelayedCommand(request)

        delayedStartService.processDelayedCommands()

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            val foundProcess = bpmnProcessService.getProcessInstancesForBusinessKey(businessKey)
            assertThat(foundProcess).hasSize(1)
        }

        val cmds = queryDelayedCommands(PROC_ID, businessKey)
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().completedAt).isNotNull()

        deleteDelayedCommand(cmds.first().ref)
    }

    @Test
    fun `processDelayedCommands should handle command with null businessKey`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = PROC_ID,
            businessKey = null,
            variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin")
        )

        // businessKey=null is saved as "" in DB
        saveDelayedCommand(request)

        delayedStartService.processDelayedCommands()

        val cmds = queryDelayedCommands(PROC_ID, "")
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().completedAt).isNotNull()

        deleteDelayedCommand(cmds.first().ref)
    }

    @Test
    fun `processDelayedCommands should increment retryCount and set lastProcRunTime on failure`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = NON_EXISTENT_PROC_ID,
            businessKey = "bk-fail-test",
            variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin")
        )

        saveDelayedCommand(request)

        val beforeProcess = Instant.now()

        delayedStartService.processDelayedCommands()

        val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-fail-test")
        assertThat(cmds).hasSize(1)

        val cmd = cmds.first()
        assertThat(cmd.retryCount).isEqualTo(1)
        assertThat(cmd.nextRetryTime).isAfter(beforeProcess)
        assertThat(cmd.lastError).contains("Exception")
        assertThat(cmd.lastError).contains("\n") // stack trace, not just message
        assertThat(cmd.lastProcRunTime).isAfterOrEqualTo(beforeProcess)
        assertThat(cmd.lastAttemptTime).isAfterOrEqualTo(beforeProcess)

        deleteDelayedCommand(cmd.ref)
    }

    @Test
    fun `processDelayedCommands should follow delay schedule on sequential failures`() {
        // Test config has "1s 2s 5s"
        // First failure: retryCount 0->1, delay = delays[1] = 2s
        // Second failure: retryCount 1->2, delay = delays[2] = 5s
        val request = StartProcessRequest(
            workspace = "",
            processId = NON_EXISTENT_PROC_ID,
            businessKey = "bk-delay-schedule",
            variables = emptyMap()
        )

        saveDelayedCommand(request)

        // First failure
        val before1 = Instant.now()
        delayedStartService.processDelayedCommands()

        var cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-delay-schedule")
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().retryCount).isEqualTo(1)
        // Next delay = delays[1] = 2s
        assertThat(cmds.first().nextRetryTime).isAfter(before1.plusSeconds(1))
        assertThat(cmds.first().nextRetryTime).isBefore(before1.plusSeconds(5))

        // Set nextRetryTime to past so it gets picked up again
        updateNextRetryTime(cmds.first().ref, Instant.now().minusSeconds(1))

        // Second failure
        val before2 = Instant.now()
        delayedStartService.processDelayedCommands()

        cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-delay-schedule")
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().retryCount).isEqualTo(2)
        // Next delay = delays[2] = 5s
        assertThat(cmds.first().nextRetryTime).isAfter(before2.plusSeconds(4))
        assertThat(cmds.first().nextRetryTime).isBefore(before2.plusSeconds(10))

        deleteDelayedCommand(cmds.first().ref)
    }

    @Test
    fun `processDelayedCommands should not process commands with future nextRetryTime`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = NON_EXISTENT_PROC_ID,
            businessKey = "bk-future-test",
            variables = emptyMap()
        )

        saveDelayedCommand(request, nextRetryTime = Instant.now().plus(Duration.ofHours(1)))

        delayedStartService.processDelayedCommands()

        val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-future-test")
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().retryCount).isEqualTo(0)

        deleteDelayedCommand(cmds.first().ref)
    }

    @Test
    fun `processDelayedCommands should skip commands with completedAt set`() {
        val request = StartProcessRequest(
            workspace = "",
            processId = NON_EXISTENT_PROC_ID,
            businessKey = "bk-skip-completed",
            variables = emptyMap()
        )

        delayedStartService.saveForDelayedRetry(request, completed = true)

        delayedStartService.processDelayedCommands()

        // Should remain untouched — retryCount still 0, no lastError
        val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-skip-completed")
        assertThat(cmds).hasSize(1)
        assertThat(cmds.first().retryCount).isEqualTo(0)
        assertThat(cmds.first().lastError).isEmpty()
        assertThat(cmds.first().completedAt).isNotNull()

        deleteDelayedCommand(cmds.first().ref)
    }

    @Test
    fun `processDelayedCommands should use last delay when retryCount exceeds configured delays`() {
        // Test config has "1s 2s 5s" => 3 delays
        // After retryCount=2 (index 2 = 5s), retryCount=3 should also use 5s (last)

        val request = StartProcessRequest(
            workspace = "",
            processId = NON_EXISTENT_PROC_ID,
            businessKey = "bk-max-delay-test",
            variables = emptyMap()
        )

        saveDelayedCommand(request, retryCount = 10)

        val beforeProcess = Instant.now()

        delayedStartService.processDelayedCommands()

        val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-max-delay-test")
        assertThat(cmds).hasSize(1)

        val cmd = cmds.first()
        assertThat(cmd.retryCount).isEqualTo(11)
        // Last configured delay is 5s, so nextRetryTime should be ~5s from now
        assertThat(cmd.nextRetryTime).isAfter(beforeProcess.plusSeconds(4))
        assertThat(cmd.nextRetryTime).isBefore(beforeProcess.plusSeconds(10))

        deleteDelayedCommand(cmd.ref)
    }

    @Test
    fun `processDelayedCommands should not process same command twice in one invocation`() {
        for (i in 1..3) {
            saveDelayedCommand(
                StartProcessRequest(
                    workspace = "",
                    processId = NON_EXISTENT_PROC_ID,
                    businessKey = "bk-dedup-test-$i",
                    variables = emptyMap()
                )
            )
        }

        delayedStartService.processDelayedCommands()

        for (i in 1..3) {
            val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-dedup-test-$i")
            assertThat(cmds).hasSize(1)
            assertThat(cmds.first().retryCount).isEqualTo(1)
            deleteDelayedCommand(cmds.first().ref)
        }
    }

    @Test
    fun `processDelayedCommands should process all records across multiple batches`() {
        val successBusinessKeys = mutableListOf<String>()
        for (i in 1..3) {
            val bk = "bk-batch-success-$i-${System.currentTimeMillis()}"
            successBusinessKeys.add(bk)
            saveDelayedCommand(
                StartProcessRequest(
                    workspace = "",
                    processId = PROC_ID,
                    businessKey = bk,
                    variables = mapOf(BPMN_WORKFLOW_INITIATOR to "admin")
                )
            )
        }
        for (i in 1..2) {
            saveDelayedCommand(
                StartProcessRequest(
                    workspace = "",
                    processId = NON_EXISTENT_PROC_ID,
                    businessKey = "bk-batch-fail-$i",
                    variables = emptyMap()
                )
            )
        }

        delayedStartService.processDelayedCommands()

        for (bk in successBusinessKeys) {
            Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                val processes = bpmnProcessService.getProcessInstancesForBusinessKey(bk)
                assertThat(processes).hasSize(1)
            }
            val cmds = queryDelayedCommands(PROC_ID, bk)
            assertThat(cmds).hasSize(1)
            assertThat(cmds.first().completedAt).isNotNull()
            deleteDelayedCommand(cmds.first().ref)
        }

        for (i in 1..2) {
            val cmds = queryDelayedCommands(NON_EXISTENT_PROC_ID, "bk-batch-fail-$i")
            assertThat(cmds).hasSize(1)
            assertThat(cmds.first().retryCount).isEqualTo(1)
            assertThat(cmds.first().completedAt).isNull()
            deleteDelayedCommand(cmds.first().ref)
        }
    }

    // endregion

    // region helpers

    private fun saveDelayedCommand(
        request: StartProcessRequest,
        retryCount: Int = 0,
        nextRetryTime: Instant = Instant.now().minusSeconds(10)
    ) {
        AuthContext.runAsSystem {
            val atts = RecordAtts(EntityRef.create(BPMN_DELAYED_START_CMD_SOURCE_ID, ""))
            atts["processId"] = request.processId
            atts["workspace"] = request.workspace
            atts["businessKey"] = request.businessKey ?: ""
            atts["variables"] = request.variables
            atts["retryCount"] = retryCount
            atts["nextRetryTime"] = nextRetryTime
            atts["lastError"] = ""
            recordsService.mutate(atts)
        }
    }

    private fun queryDelayedCommands(
        processId: String,
        businessKey: String? = null
    ): List<DelayedCmdRecord> {
        return AuthContext.runAsSystem {
            val predicate = if (businessKey != null) {
                Predicates.and(
                    Predicates.eq("processId", processId),
                    Predicates.eq("businessKey", businessKey)
                )
            } else {
                Predicates.eq("processId", processId)
            }
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BPMN_DELAYED_START_CMD_SOURCE_ID)
                    withLanguage("predicate")
                    withQuery(predicate)
                },
                DelayedCmdRecord::class.java
            ).getRecords()
        }
    }

    private fun updateNextRetryTime(ref: EntityRef, nextRetryTime: Instant) {
        AuthContext.runAsSystem {
            val atts = RecordAtts(ref)
            atts["nextRetryTime"] = nextRetryTime
            recordsService.mutate(atts)
        }
    }

    private fun deleteDelayedCommand(ref: EntityRef) {
        AuthContext.runAsSystem {
            recordsService.delete(ref)
        }
    }

    // endregion

    @AfterAll
    fun tearDown() {
        helper.cleanDeployments()
        helper.cleanDefinitions()
        AuthContext.runAsSystem {
            val all = recordsService.query(
                RecordsQuery.create {
                    withSourceId(BPMN_DELAYED_START_CMD_SOURCE_ID)
                    withLanguage("predicate")
                    withQuery(Predicates.alwaysTrue())
                    withMaxItems(1000)
                }
            ).getRecords()
            all.forEach { recordsService.delete(it) }
        }
    }

    data class DelayedCmdRecord(
        @AttName("?id")
        val ref: EntityRef = EntityRef.EMPTY,
        val processId: String = "",
        val workspace: String = "",
        val businessKey: String = "",
        val variables: ObjectData = ObjectData.create(),
        val retryCount: Int = 0,
        val nextRetryTime: Instant = Instant.EPOCH,
        val completedAt: Instant? = null,
        val lastProcRunTime: Instant? = null,
        val lastAttemptTime: Instant? = null,
        val lastError: String = ""
    )
}
