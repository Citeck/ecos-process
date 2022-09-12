package ru.citeck.ecos.process.domain.bpmn.elements

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.ProcessEngineException
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat
import org.camunda.bpm.scenario.ProcessScenario
import org.camunda.bpm.scenario.Scenario.run
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.internal.verification.VerificationModeFactory.times
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

private const val USER_IVAN = "ivan.petrov"
private const val USER_PETR = "petr.ivanov"
private const val USER_4_LOOP = "user4Loop"

private const val GROUP_MANAGER = "GROUP_manager"
private const val GROUP_DEVELOPER = "GROUP_developer"

private const val USER_TASK = "usertask"
private const val GATEWAY = "gateway"

/**
 * Why all tests places on one class? TODO: fill
 * *
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnMonsterElementsTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Mock
    private lateinit var process: ProcessScenario

    @MockBean
    private lateinit var camundaRoleService: CamundaRoleService

    @MockBean
    private lateinit var statusSetter: CamundaStatusSetter

    private val variables = mapOf(
        "documentRef" to "doc@1"
    )

    // ---BPMN USER TASK TESTS ---

    @BeforeEach
    fun setUp() {
        `when`(camundaRoleService.getUserNames(any(), any())).thenReturn(listOf(USER_IVAN, USER_PETR))
        `when`(camundaRoleService.getGroupNames(any(), any())).thenReturn(listOf(GROUP_MANAGER, GROUP_DEVELOPER))
        `when`(camundaRoleService.getAuthorityNames(any(), any())).thenReturn(
            listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)
        )
    }

    @Test
    fun `complete simple task`() {
        val procId = "test-user-task-role"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task form ref check`() {
        val procId = "test-user-task-role"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasFormKey("uiserv/form@test-bpmn-form-task")
            it.complete()
        }

        run(process).startByKey(procId, variables).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task priority check`() {
        val procId = "test-user-task-priority"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it.priority).isEqualTo(TaskPriority.HIGH.toCamundaCode())
            it.complete()
        }

        run(process).startByKey(procId, variables).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment by roles`() {
        val procId = "test-user-task-role"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            assertThat(it).hasCandidateGroup(GROUP_MANAGER)
            assertThat(it).hasCandidateGroup(GROUP_DEVELOPER)
            it.complete()
        }

        run(process).startByKey(procId, variables).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task multi instance auto mode - assignment by roles`() {
        val procId = "test-user-task-role-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                val count = taskService.createTaskQuery().active().count()
                assertThat(count).isEqualTo(1)

                assertThat(it).isAssignedTo(USER_IVAN)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_PETR)
                it.complete()
            },
            {
                assertThat(it).hasCandidateGroup(GROUP_MANAGER)
                it.complete()
            }
        )

        run(process).startByKey(procId, variables).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task multi instance parallel auto mode - assignment by roles`() {
        val procId = "test-user-task-role-multi-instance-parallel"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(3)

                assertThat(it).isAssignedTo(USER_IVAN)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_PETR)
                it.complete()
            },
            {
                assertThat(it).hasCandidateGroup(GROUP_MANAGER)
                it.complete()
            }
        )

        run(process).startByKey(procId, variables).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance collection`() {
        val procId = "test-user-task-manual-collection-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(1)

                assertThat(it).hasCandidateUser(USER_IVAN)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_PETR)
                it.complete()
            },
            {
                assertThat(it).hasCandidateGroup(GROUP_MANAGER)
                it.complete()
            }
        )

        run(process).startByKey(
            procId,
            mapOf("recipients" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER))
        ).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance parallel collection`() {
        val procId = "test-user-task-manual-collection-multi-instance-parallel"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(3)

                assertThat(it).hasCandidateUser(USER_IVAN)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_PETR)
                it.complete()
            },
            {
                assertThat(it).hasCandidateGroup(GROUP_MANAGER)
                it.complete()
            }
        )

        run(process).startByKey(
            procId,
            mapOf("recipients" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER))
        ).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance loop`() {
        val procId = "test-user-task-manual-loop-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(1)

                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            }
        )

        run(process).startByKey(procId).execute()

        verify(process, times(4)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance loop parallel`() {
        val procId = "test-user-task-manual-loop-multi-instance-parallel"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(4)

                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).hasCandidateUser(USER_4_LOOP)
                it.complete()
            }
        )

        run(process).startByKey(procId).execute()

        verify(process, times(4)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual`() {
        val procId = "test-user-task-assign-manual"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            assertThat(it).hasCandidateGroup(GROUP_MANAGER)
            assertThat(it).hasCandidateGroup(GROUP_DEVELOPER)
            it.complete()
        }

        run(process).startByKey(procId, variables).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual with expressions`() {
        val procId = "test-user-task-assign-manual-with-expressions"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)
            assertThat(it).hasCandidateUser("scriptUserFullRef")
            assertThat(it).hasCandidateUser("scriptUser")
            assertThat(it).hasCandidateUser("userFromVariable")

            assertThat(it).hasCandidateGroup("GROUP_company_chief_accountant")
            assertThat(it).hasCandidateGroup("GROUP_company_accountant")
            assertThat(it).hasCandidateGroup("GROUP_fromVariable")

            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "recipientsFromIncomeProcessVariables" to listOf("userFromVariable", "GROUP_fromVariable")
            )
        ).execute()

        verify(process).hasFinished("endEvent")
    }

    // ---BPMN SCRIPT TASK TESTS ---

    @Test
    fun `script task set variables`() {
        val procId = "test-script-task-set-variables"
        saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "foo" to "foo"
            )
        ).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("fooBar", "foo bar")
        assertThat(scenario.instance(process)).variables().containsEntry("newVariable", "new var from script")

        verify(process).hasFinished("endEvent")
    }

    // ---BPMN GATEWAY TESTS ---

    @Test
    fun `gateway condition javascript compare variable variant top`() {
        val procId = "test-gateway-condition-javascript"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "top",
            )
        ).execute()

        verify(process).hasFinished("endTop")
    }

    @Test
    fun `gateway condition javascript compare variable variant bottom`() {
        val procId = "test-gateway-condition-javascript"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "bottom",
            )
        ).execute()

        verify(process).hasFinished("endBottom")
    }

    @Test
    fun `gateway condition javascript none of the conditions are true, should go to default flow`() {
        val procId = "test-gateway-condition-javascript"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "no-one-flow",
            )
        ).execute()

        verify(process).hasFinished("endDefault")
    }

    @Test
    fun `gateway condition expression compare variable variant top`() {
        val procId = "test-gateway-condition-expression"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "top",
            )
        ).execute()

        verify(process).hasFinished("endTop")
    }

    @Test
    fun `gateway condition expression compare variable variant bottom`() {
        val procId = "test-gateway-condition-expression"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "bottom",
            )
        ).execute()

        verify(process).hasFinished("endBottom")
    }

    @Test
    fun `gateway condition expression none of the conditions are true, should go to default flow`() {
        val procId = "test-gateway-condition-expression"
        saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "flow" to "no-one-flow",
            )
        ).execute()

        verify(process).hasFinished("endDefault")
    }

    @Test
    fun `gateway condition task outcome variant done`() {
        val procId = "test-gateway-condition-task-outcome"
        saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "done", MLText.EMPTY)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                taskOutcome.outcomeId() to taskOutcome.value,
            )
        ).execute()

        verify(process).hasFinished("endDone")
    }

    @Test
    fun `gateway condition task outcome variant cancel`() {
        val procId = "test-gateway-condition-task-outcome"
        saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "cancel", MLText.EMPTY)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                taskOutcome.outcomeId() to taskOutcome.value,
            )
        ).execute()

        verify(process).hasFinished("endCancel")
    }

    @Test
    fun `gateway condition task outcome, complete task with incorrect outcome should throw exception`() {
        val procId = "test-gateway-condition-task-outcome"
        saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "incorrect_task_outcome", MLText.EMPTY)

        assertThrows<ProcessEngineException> {
            `when`(process.waitsAtUserTask("userTask")).thenReturn {
                it.complete()
            }

            run(process).startByKey(
                procId,
                mapOf(
                    "documentRef" to "doc@1",
                    taskOutcome.outcomeId() to taskOutcome.value,
                )
            ).execute()
        }
    }

    // --- BPMN SET STATUS TESTS ---

    @Test
    fun `set status of document`() {
        val procId = "test-set-status"
        saveAndDeployBpmn("status", procId)

        val docRef = RecordRef.valueOf("doc@1")

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to docRef.toString()
            )
        ).execute()

        verify(process).hasFinished("end")
        verify(statusSetter, Mockito.times(1)).setStatus(docRef, "approval")
    }

    private fun getActiveTasksCountForProcess(procId: String): Long {
        return taskService.createTaskQuery()
            .processDefinitionKey(procId)
            .active()
            .count()
    }
}
