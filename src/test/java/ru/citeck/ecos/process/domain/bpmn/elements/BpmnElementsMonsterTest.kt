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
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.internal.verification.VerificationModeFactory.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

private const val USER_IVAN = "ivan.petrov"
private const val USER_PETR = "petr.ivanov"
private const val USER_4_LOOP = "user4Loop"

private const val GROUP_MANAGER = "GROUP_manager"
private const val GROUP_DEVELOPER = "GROUP_developer"

private const val USER_TASK = "usertask"
private const val GATEWAY = "gateway"
private const val SUB_PROCESS = "subprocess"
private const val SEND_TASK = "sendtask"

/**
 * Why all tests places on one monster class?
 * Tests begin to behave unpredictably if placed in different classes. DirtiesContext and TestInstance.Lifecycle
 * does not help.
 *
 * Similar case - https://forum.camunda.io/t/tests-instable/13960
 *
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnElementsMonsterTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Mock
    private lateinit var process: ProcessScenario

    @SpyBean
    private lateinit var camundaRoleService: CamundaRoleService

    @MockBean
    private lateinit var statusSetter: CamundaStatusSetter

    @MockBean
    private lateinit var notificationService: NotificationService

    companion object {
        private val harryRecord = PotterRecord()
        private val harryRef = RecordRef.valueOf("hogwarts/people@harry")
        private val docRef = RecordRef.valueOf("doc@1")

        private val variables = mapOf(
            "documentRef" to "doc@1"
        )

        private val mockRoleUserNames = listOf(USER_IVAN, USER_PETR)
        private val mockRoleGroupNames = listOf(GROUP_MANAGER, GROUP_DEVELOPER)
        private val mockRoleAuthorityNames = listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)

        private val mockAuthorEmails = listOf("$USER_IVAN@mail.com", "$USER_PETR@mail.com")
    }


    // ---BPMN USER TASK TESTS ---

    @BeforeEach
    fun setUp() {
        recordsService.register(
            RecordsDaoBuilder.create("hogwarts/people")
                .addRecord(
                    harryRef.id,
                    PotterRecord()
                )
                .build()
        )

        `when`(camundaRoleService.getUserNames(anyString(), anyString())).thenReturn(mockRoleUserNames)
        `when`(camundaRoleService.getGroupNames(anyString(), anyString())).thenReturn(mockRoleGroupNames)
        `when`(camundaRoleService.getAuthorityNames(anyString(), anyString())).thenReturn(mockRoleAuthorityNames)

        `when`(camundaRoleService.getEmails(docRef, listOf("author"))).thenReturn(mockAuthorEmails)

        `when`(camundaRoleService.getKey()).thenReturn(CamundaRoleService.KEY)
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

    @Test
    fun `script task get document variables`() {
        val procId = "test-script-task-get-document-variables"
        saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to harryRef.toString()
            )
        ).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("loadedName", harryRecord.name)
        assertThat(scenario.instance(process)).variables().containsEntry("loadedEmail", harryRecord.email)

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `script task check role service`() {
        val procId = "test-script-task-check-role-service"
        saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to harryRef.toString()
            )
        ).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("userNames", mockRoleUserNames)
        assertThat(scenario.instance(process)).variables().containsEntry("groupNames", mockRoleGroupNames)
        assertThat(scenario.instance(process)).variables().containsEntry("authorityNames", mockRoleAuthorityNames)

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

    // --- BPMN SUB PROCESS TESTS ---

    @Test
    fun `sub process simple`() {
        val procId = "test-sub-process"
        saveAndDeployBpmn(SUB_PROCESS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            assertThat(it).hasCandidateGroup(GROUP_MANAGER)
            assertThat(it).hasCandidateGroup(GROUP_DEVELOPER)

            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1"
            )
        ).execute()

        verify(process).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `sub process multi instance sequential`() {
        val procId = "test-sub-process-multi-instance-sequential"
        saveAndDeployBpmn(SUB_PROCESS, procId)

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
            mapOf(
                "documentRef" to "doc@1",
                "testCandidateCollection" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)
            )
        ).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process, times(3)).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `sub process multi instance parallel`() {
        val procId = "test-sub-process-multi-instance-parallel"
        saveAndDeployBpmn(SUB_PROCESS, procId)

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
            mapOf(
                "documentRef" to "doc@1",
                "testCandidateCollection" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)
            )
        ).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process, times(3)).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    // --- BPMN SEND TASK TESTS ---
    @Test
    fun `send task with template`() {
        val procId = "test-send-task-with-template"
        saveAndDeployBpmn(SEND_TASK, procId)

        val docRef = RecordRef.valueOf("doc@1")

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to docRef.toString()
            )
        ).execute()

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(RecordRef.valueOf("notifications/template@test-template"))
            .build()

        verify(notificationService).send(org.mockito.kotlin.check {
            assertThat(NotificationEqualsWrapper(it)).isEqualTo(NotificationEqualsWrapper(notification))
        })

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

}

class PotterRecord(

    @AttName("email")
    val email: String = "harry.potter@hogwarts.com",

    @AttName("name")
    val name: String = "Harry Potter"

)

/**
 * Wrap equals without id
 */
private data class NotificationEqualsWrapper(
    val dto: Notification
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationEqualsWrapper

        if (dto.record != other.dto.record) return false
        if (dto.title != other.dto.title) return false
        if (dto.body != other.dto.body) return false
        if (dto.templateRef != other.dto.templateRef) return false
        if (dto.type != other.dto.type) return false
        if (dto.recipients != other.dto.recipients) return false
        if (dto.from != other.dto.from) return false
        if (dto.cc != other.dto.cc) return false
        if (dto.bcc != other.dto.bcc) return false
        if (dto.lang != other.dto.lang) return false
        if (dto.additionalMeta != other.dto.additionalMeta) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dto.record.hashCode()
        result = 31 * result + dto.title.hashCode()
        result = 31 * result + dto.body.hashCode()
        result = 31 * result + dto.templateRef.hashCode()
        result = 31 * result + dto.type.hashCode()
        result = 31 * result + dto.recipients.hashCode()
        result = 31 * result + dto.cc.hashCode()
        result = 31 * result + dto.from.hashCode()
        result = 31 * result + dto.bcc.hashCode()
        result = 31 * result + dto.lang.hashCode()
        result = 31 * result + dto.additionalMeta.hashCode()
        return result
    }
}
