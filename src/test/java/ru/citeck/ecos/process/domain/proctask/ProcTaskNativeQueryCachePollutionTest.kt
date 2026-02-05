package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

/**
 * Test to reproduce the issue where NativeTaskQuery pollutes DbEntityCache
 * with incomplete TaskEntity objects (revision=0, executionId=null).
 *
 * Bug description:
 * When ProcTaskSqlQueryBuilder executes NativeTaskQuery with "SELECT DISTINCT T.ID_",
 * MyBatis creates TaskEntity with only 'id' field populated. This incomplete TaskEntity
 * is added to DbEntityCache. Later, when getVariables() is called, it retrieves
 * the incomplete TaskEntity from cache, and since executionId=null,
 * getParentVariableScope() returns null, causing process variables to be excluded.
 *
 * @see ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskNativeQueryCachePollutionTest {

    @Autowired
    private lateinit var processEngineConfiguration: ProcessEngineConfigurationImpl

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var repositoryService: RepositoryService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    companion object {
        private const val PROC_KEY = "native-query-cache-test"
        private const val PROCESS_VAR_1 = "processVar1"
        private const val PROCESS_VAR_2 = "processVar2"
    }

    private var deploymentId: String? = null
    private var processInstanceId: String? = null

    @BeforeEach
    fun setUp() {
        val modelInstance = Bpmn.createExecutableProcess(PROC_KEY)
            .startEvent()
            .userTask("userTask1")
            .name("Test Task")
            .endEvent()
            .done()

        val deployment = repositoryService.createDeployment()
            .addModelInstance("$PROC_KEY.bpmn", modelInstance)
            .deploy()

        deploymentId = deployment.id
    }

    @AfterEach
    fun tearDown() {
        processInstanceId?.let { pid ->
            try {
                runtimeService.deleteProcessInstance(pid, "test cleanup")
            } catch (_: Exception) {
            }
        }
        deploymentId?.let { depId ->
            try {
                repositoryService.deleteDeployment(depId, true)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * This test verifies that ProcTaskService.findTasks() does NOT pollute
     * DbEntityCache with incomplete TaskEntity objects.
     *
     * After the fix, ProcTaskSqlQueryBuilder (used by ProcTaskService) uses raw JDBC
     * instead of NativeTaskQuery, so no TaskEntity objects are created and the cache
     * remains clean.
     *
     * This simulates what happens when Records.query("proc-task", ...) is called
     * from a Script Task in Camunda - both the query and subsequent getVariables()
     * execute within the SAME CommandContext.
     *
     * @see ru.citeck.ecos.process.domain.proctask.service.ProcTaskService#findTasks
     */
    @Test
    fun `ProcTaskService findTasks should not pollute cache causing getVariables to return incomplete data`() {
        // Start process with variables
        val processInstance = runtimeService.startProcessInstanceByKey(
            PROC_KEY,
            mapOf(
                PROCESS_VAR_1 to "value1",
                PROCESS_VAR_2 to "value2"
            )
        )
        processInstanceId = processInstance.id

        // Get task ID in separate transaction first
        val task = taskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .singleResult()

        assertThat(task).isNotNull
        val taskId = task.id

        // Execute within single CommandContext (simulating Script Task execution)
        val commandExecutor = processEngineConfiguration.commandExecutorTxRequired

        val variables = commandExecutor.execute { _ ->
            // Step 1: Call ProcTaskService.findTasks() - this is what Records.query() uses
            // After fix, this uses raw JDBC and does NOT create TaskEntity objects
            val result = AuthContext.runAsSystem {
                procTaskService.findTasks(Predicates.alwaysTrue())
            }

            assertThat(result.entities).contains(taskId)

            // Step 2: Now try to get variables for the same task
            // Since ProcTaskService uses JDBC (not NativeTaskQuery),
            // no TaskEntity was added to cache, so getVariables should work correctly
            taskService.getVariables(taskId)
        }

        // Verify that process variables are included
        assertThat(variables)
            .withFailMessage(
                "Process variable '$PROCESS_VAR_1' not found in variables for task $taskId. " +
                    "This indicates cache was polluted with incomplete TaskEntity " +
                    "(executionId=null, revision=0). Available variables: ${variables.keys}"
            )
            .containsKey(PROCESS_VAR_1)

        assertThat(variables)
            .withFailMessage(
                "Process variable '$PROCESS_VAR_2' not found in variables for task $taskId."
            )
            .containsKey(PROCESS_VAR_2)

        assertThat(variables[PROCESS_VAR_1]).isEqualTo("value1")
        assertThat(variables[PROCESS_VAR_2]).isEqualTo("value2")
    }

    /**
     * This test documents the original bug where NativeTaskQuery pollutes DbEntityCache.
     *
     * When NativeTaskQuery is used with "SELECT DISTINCT T.ID_", MyBatis creates
     * incomplete TaskEntity objects that pollute the cache. This test confirms
     * the bug still exists in Camunda's NativeTaskQuery (which is why we use JDBC).
     */
    @Test
    fun `NativeTaskQuery still pollutes cache - documents Camunda bug`() {
        // Start process with variables
        val processInstance = runtimeService.startProcessInstanceByKey(
            PROC_KEY,
            mapOf(
                PROCESS_VAR_1 to "value1",
                PROCESS_VAR_2 to "value2"
            )
        )
        processInstanceId = processInstance.id

        // Get task ID in separate transaction first
        val task = taskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .singleResult()

        assertThat(task).isNotNull
        val taskId = task.id

        // Execute within single CommandContext
        val commandExecutor = processEngineConfiguration.commandExecutorTxRequired

        val variables = commandExecutor.execute { _ ->
            // Use NativeTaskQuery directly (the old buggy approach)
            val nativeQuery = taskService.createNativeTaskQuery()
                .sql("SELECT DISTINCT T.ID_ FROM ACT_RU_TASK T WHERE T.ID_ = #{taskId}")
                .parameter("taskId", taskId)

            val tasksFromNative = nativeQuery.list()
            assertThat(tasksFromNative).hasSize(1)
            assertThat(tasksFromNative[0].id).isEqualTo(taskId)

            taskService.getVariables(taskId)
        }

        // This documents the BUG: NativeTaskQuery pollutes cache and process variables are missing
        assertThat(variables)
            .withFailMessage(
                "Expected NativeTaskQuery to pollute cache (Camunda bug), " +
                    "but process variables were found. Has Camunda fixed this bug?"
            )
            .doesNotContainKey(PROCESS_VAR_1)
    }

    /**
     * This test verifies that getVariables works correctly when called
     * in a SEPARATE CommandContext (normal case, like from browser).
     * Each Camunda API call creates new CommandContext with empty cache.
     */
    @Test
    fun `getVariables should return process variables in separate CommandContext`() {
        // Start process with variables
        val processInstance = runtimeService.startProcessInstanceByKey(
            PROC_KEY,
            mapOf(
                PROCESS_VAR_1 to "value1",
                PROCESS_VAR_2 to "value2"
            )
        )
        processInstanceId = processInstance.id

        // Get task ID
        val task = taskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .singleResult()

        assertThat(task).isNotNull
        val taskId = task.id

        // Execute in separate CommandContexts (normal case - each call is separate)
        // First: NativeTaskQuery (creates its own CommandContext)
        val tasksFromNative = taskService.createNativeTaskQuery()
            .sql("SELECT DISTINCT T.ID_ FROM ACT_RU_TASK T WHERE T.ID_ = #{taskId}")
            .parameter("taskId", taskId)
            .list()

        assertThat(tasksFromNative).hasSize(1)

        // Second: getVariables (creates new CommandContext - cache is clean)
        val variables = taskService.getVariables(taskId)

        // This should work correctly because it's a new CommandContext
        assertThat(variables).containsKey(PROCESS_VAR_1)
        assertThat(variables).containsKey(PROCESS_VAR_2)
    }

    /**
     * This test verifies the expected behavior of TaskEntity loaded via NativeQuery.
     * When SELECT returns only ID_, the TaskEntity should have:
     * - id = value from DB
     * - revision = 0 (default, not loaded)
     * - executionId = null (not loaded)
     */
    @Test
    fun `NativeTaskQuery with SELECT ID only creates TaskEntity with null executionId`() {
        // Start process with variables
        val processInstance = runtimeService.startProcessInstanceByKey(
            PROC_KEY,
            mapOf(PROCESS_VAR_1 to "value1")
        )
        processInstanceId = processInstance.id

        // Get task ID
        val task = taskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .singleResult()

        assertThat(task).isNotNull
        val taskId = task.id

        // Verify that normal TaskQuery returns complete TaskEntity
        assertThat(task.executionId).isNotNull
        assertThat(task.processInstanceId).isEqualTo(processInstance.id)

        // Now execute NativeTaskQuery and check what TaskEntity we get
        val commandExecutor = processEngineConfiguration.commandExecutorTxRequired

        val (nativeTaskExecutionId, nativeTaskRevision) = commandExecutor.execute { _ ->
            val nativeQuery = taskService.createNativeTaskQuery()
                .sql("SELECT T.ID_ FROM ACT_RU_TASK T WHERE T.ID_ = #{taskId}")
                .parameter("taskId", taskId)

            val nativeTask = nativeQuery.singleResult()

            // Check the state of TaskEntity from NativeQuery
            val taskEntity = nativeTask as TaskEntity

            Pair(taskEntity.executionId, taskEntity.revision)
        }

        // This documents the BUG: NativeTaskQuery creates incomplete TaskEntity
        // In a fixed version, these assertions should be:
        // assertThat(nativeTaskExecutionId).isNotNull()
        // assertThat(nativeTaskRevision).isGreaterThan(0)
        //
        // But currently (bug):
        assertThat(nativeTaskExecutionId)
            .withFailMessage(
                "BUG CONFIRMED: NativeTaskQuery with SELECT ID_ only creates TaskEntity with executionId=null. " +
                    "This causes getVariables() to not return process variables when called in same CommandContext."
            )
            .isNull()

        assertThat(nativeTaskRevision)
            .withFailMessage(
                "BUG CONFIRMED: NativeTaskQuery with SELECT ID_ only creates TaskEntity with revision=0."
            )
            .isEqualTo(0)
    }
}
