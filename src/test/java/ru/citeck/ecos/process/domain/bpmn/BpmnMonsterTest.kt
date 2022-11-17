package ru.citeck.ecos.process.domain.bpmn

import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ProcessEngineException
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat
import org.camunda.bpm.scenario.ProcessScenario
import org.camunda.bpm.scenario.Scenario.run
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.internal.verification.VerificationModeFactory.times
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_BUSINESS_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.deleteAllProcDefinitions
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicate
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
private const val POOL = "pool"
private const val GATEWAY = "gateway"
private const val SUB_PROCESS = "subprocess"
private const val SEND_TASK = "sendtask"
private const val TIMER = "timer"
private const val SUBSCRIPTION = "subscription"

/**
 * Why all tests places on one monster class?
 * Tests that start the process begin to behave unpredictably if placed in different classes.
 * DirtiesContext and TestInstance.Lifecycle does not help.
 *
 * Similar case - https://forum.camunda.io/t/tests-instable/13960
 *
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnMonsterTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    @Autowired
    private lateinit var camundaHistoryService: HistoryService

    @Autowired
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    @Autowired
    private lateinit var procHistoricTaskService: ProcHistoricTaskService

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
        private val docExplicitRef = RecordRef.valueOf("doc@explicit")

        private val variables_docRef = mapOf(
            "documentRef" to "doc@1"
        )

        private val mockRoleUserNames = listOf(USER_IVAN, USER_PETR)
        private val mockRoleGroupNames = listOf(GROUP_MANAGER, GROUP_DEVELOPER)
        private val mockRoleAuthorityNames = listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)

        private val mockAuthorEmails = listOf("$USER_IVAN@mail.com", "$USER_PETR@mail.com")
        private val mockInitiatorEmails = listOf("initiator@mail.com")
        private val mockApproverEmails = listOf("approver@mail.com")
        private val mockAuthorAccountantEmails = listOf("author@mail.com", "accountant@mail.com")
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
        `when`(camundaRoleService.getEmails(docExplicitRef, listOf("author"))).thenReturn(mockAuthorEmails)
        `when`(camundaRoleService.getEmails(docRef, listOf("initiator"))).thenReturn(mockInitiatorEmails)
        `when`(camundaRoleService.getEmails(docRef, listOf("approver"))).thenReturn(mockApproverEmails)
        `when`(camundaRoleService.getEmails(docRef, listOf("author", "accountant"))).thenReturn(
            mockAuthorAccountantEmails
        )

        `when`(camundaRoleService.getKey()).thenReturn(CamundaRoleService.KEY)
    }

    @AfterEach
    fun clearDefinitions() {
        deleteAllProcDefinitions()
    }

    @Test
    fun `complete simple task`() {
        val procId = "test-user-task-role"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).execute()

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

        run(process).startByKey(procId, variables_docRef).execute()

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

        run(process).startByKey(procId, variables_docRef).execute()

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

        run(process).startByKey(procId, variables_docRef).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task multi instance auto mode - assignment by roles`() {
        val procId = "test-user-task-role-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(1)

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

        run(process).startByKey(procId, variables_docRef).execute()

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

        run(process).startByKey(procId, variables_docRef).execute()

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

                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
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

                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
                it.complete()
            },
            {
                assertThat(it).isAssignedTo(USER_4_LOOP)
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

        run(process).startByKey(procId, variables_docRef).execute()

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

    @Test
    fun `task assignment manual single recipient with expressions`() {
        val procId = "test-user-task-assign-single-manual-with-expressions"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser("userFromVariable")

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

    @Test
    fun `task ml text name`() {
        val procId = "test-user-task-ml-name"
        saveAndDeployBpmn(USER_TASK, procId)

        val doc = "doc@ml"

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val task = procTaskService.getTasksByDocument(doc).first()
            val expectedName = MLText(
                mapOf(
                    LocaleUtils.toLocale("ru") to "Пользовательская задача",
                    LocaleUtils.toLocale("en") to "User task"
                )
            )

            it.complete()

            assertThat(task.name).isEqualTo(expectedName)
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to doc
            )
        ).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task ml text name single lang`() {
        val procId = "test-user-task-ml-name-single-lang"
        saveAndDeployBpmn(USER_TASK, procId)

        val doc = "doc@ml-single-lang"

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val task = procTaskService.getTasksByDocument(doc).first()
            val expectedName = MLText(
                mapOf(
                    LocaleUtils.toLocale("en") to "User task"
                )
            )

            it.complete()

            assertThat(task.name).isEqualTo(expectedName)
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to doc
            )
        ).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task empty json ml text should return default name`() {
        val procId = "test-user-task-ml-empty-json"
        saveAndDeployBpmn(USER_TASK, procId)

        val doc = "doc@ml-empty-json"

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val task = procTaskService.getTasksByDocument(doc).first()
            val expectedName = MLText(
                mapOf(
                    LocaleUtils.toLocale("en") to "Пользовательская задача"
                )
            )

            it.complete()

            assertThat(task.name).isEqualTo(expectedName)
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to doc
            )
        ).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task blank ml text should return default name`() {
        val procId = "test-user-task-ml-blank"
        saveAndDeployBpmn(USER_TASK, procId)

        val doc = "doc@ml-blank"

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val task = procTaskService.getTasksByDocument(doc).first()
            val expectedName = MLText(
                mapOf(
                    LocaleUtils.toLocale("en") to "Пользовательская задача"
                )
            )

            it.complete()

            assertThat(task.name).isEqualTo(expectedName)
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to doc
            )
        ).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task without json ml text should return default name`() {
        val procId = "test-user-task-without-ml-json"
        saveAndDeployBpmn(USER_TASK, procId)

        val doc = "doc@without-ml-json"

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val task = procTaskService.getTasksByDocument(doc).first()
            val expectedName = MLText(
                mapOf(
                    LocaleUtils.toLocale("en") to "Пользовательская задача"
                )
            )

            it.complete()

            assertThat(task.name).isEqualTo(expectedName)
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to doc
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
    fun `script task can running without document ref`() {
        val procId = "test-script-task-set-variables"
        saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
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

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(NotificationEqualsWrapper(notification))
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `send task with explicit text`() {
        val procId = "test-send-task-with-explicit-text"
        saveAndDeployBpmn(SEND_TASK, procId)

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
            .title("Hello Ivan")
            .body("<p>Hello Ivan, your document is approved</p>")
            .build()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(NotificationEqualsWrapper(notification))
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `send task recipients check`() {
        val procId = "test-send-task-recipients-check"
        saveAndDeployBpmn(SEND_TASK, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to docRef.toString()
            )
        ).execute()

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorAccountantEmails)
            .cc(mockInitiatorEmails)
            .bcc(mockApproverEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(RecordRef.valueOf("notifications/template@test-template"))
            .build()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(NotificationEqualsWrapper(notification))
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `send task configuration check`() {
        val procId = "test-send-task-configuration-check"
        saveAndDeployBpmn(SEND_TASK, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to docRef.toString()
            )
        ).execute()

        val notification = Notification.Builder()
            .record(docExplicitRef)
            .recipients(mockAuthorEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("en")
            .additionalMeta(
                mapOf(
                    "foo" to "bar",
                    "harry" to "potter"
                )
            )
            .templateRef(RecordRef.valueOf("notifications/template@test-template"))
            .build()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(NotificationEqualsWrapper(notification))
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    // --- BPMN TIMER TESTS ---

    @Test
    fun `timer boundary non interrupting`() {
        val procId = "test-timer-boundary-non-interrupting"
        saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P2DT12H") { task.complete() }
        }

        run(process).startByKey(procId, variables_docRef).execute()

        verify(process, times(2)).hasFinished("remindSendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `timer value from expression`() {
        val procId = "test-timer-value-from-expression"
        saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P4DT12H") { task.complete() }
        }

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to "doc@1",
                "timeValue" to "R/P1D"
            )
        ).execute()

        verify(process, times(4)).hasFinished("remindSendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `timer boundary duration interrupting - done task flow`() {
        val procId = "test-timer-boundary-duration-interrupting"
        saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).execute()

        verify(process).hasFinished("taskDoneFlow")
        verify(process, never()).hasFinished("taskExpiredFlow")
    }

    @Test
    fun `timer boundary duration interrupting - expired task flow`() {
        val procId = "test-timer-boundary-duration-interrupting"
        saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P3DT12H") { }
        }

        run(process).startByKey(procId, variables_docRef).execute()

        assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(0)

        verify(process).hasCompleted("expiredSendTask")
        verify(process).hasFinished("taskExpiredFlow")
        verify(process, never()).hasFinished("taskDoneFlow")
    }

    @Test
    fun `timer intermediate catch`() {
        val procId = "test-timer-intermediate-catch"
        saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtTimerIntermediateEvent("timer")).thenReturn {
            // Do nothing means process moves forward because of the timers
        }
        `when`(process.waitsAtUserTask("userTask")).thenReturn { task ->
            task.complete()
        }

        run(process).startByKey(procId, variables_docRef).execute()

        verify(process).hasFinished("endEvent")
    }

    // --- BPMN HISTORIC TASKS TESTS ---

    @Test
    fun `historic tasks order validation`() {
        val procId = "test-user-task-role-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                it.complete()
            },
            {
                it.complete()
            },
            {
                it.complete()
            }
        )

        val scenario = run(process).startByKey(procId, variables_docRef).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")

        val procInstance = scenario.instance(process)

        val historicTasks = camundaHistoryService.createHistoricTaskInstanceQuery()
            .orderByHistoricTaskInstanceEndTime()
            .desc()
            .processInstanceId(procInstance.id)
            .finished()
            .list()
            .map { it.id }

        val historicTaskFromService = procHistoricTaskService.getHistoricTasksByIds(historicTasks)
            .map { it?.id }

        assertThat(historicTaskFromService).isEqualTo(historicTasks)
    }

    @Test
    fun `historic tasks order validation with not exists tasks`() {
        val notExistsTask = "notExistsTask"
        val procId = "test-user-task-role-multi-instance"
        saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn(
            {
                it.complete()
            },
            {
                it.complete()
            },
            {
                it.complete()
            }
        )

        val scenario = run(process).startByKey(procId, variables_docRef).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")

        val procInstance = scenario.instance(process)

        val historicTasks = camundaHistoryService.createHistoricTaskInstanceQuery()
            .orderByHistoricTaskInstanceEndTime()
            .desc()
            .processInstanceId(procInstance.id)
            .finished()
            .list()
            .map { it.id }
            .toMutableList()
        historicTasks.add(notExistsTask)

        val historicTaskFromService = procHistoricTaskService.getHistoricTasksByIds(historicTasks)
            .map { it?.id }

        assertThat(historicTaskFromService).hasSize(4)
        assertThat(historicTaskFromService).containsAll(historicTasks.filter { it != notExistsTask })
        assertThat(historicTaskFromService[3]).isNull()
    }

    // --- BPMN POOL PARTICIPANTS TESTS ---

    @Test
    fun `pool with single participant and multiple lines - check call all activities on lines`() {
        val procId = "test-pool-single-participants-with-multiple-lines"
        saveAndDeployBpmn(POOL, procId)

        `when`(process.waitsAtUserTask("userTask_1")).thenReturn {
            it.complete()
        }

        `when`(process.waitsAtUserTask("userTask_2")).thenReturn {
            it.complete()
        }

        val scenario = run(process).startByKey(
            procId,
            variables_docRef
        ).execute()

        verify(process, times(1)).hasCompleted("userTask_1")
        verify(process, times(1)).hasCompleted("userTask_2")
        verify(process, times(1)).hasCompleted("scriptTask")

        assertThat(scenario.instance(process)).variables().containsEntry("foo", "bar")

        verify(process).hasFinished("endEvent")
    }

    // --- BPMN EVENT SUBSCRIPTION FINDER TESTS ---

    @Test
    fun `find all deployed subscriptions with different versions payload`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdModified = "test-subscriptions-start-signal-event_2"

        saveAndDeployBpmn(SUBSCRIPTION, procId)
        saveAndDeployBpmn(SUBSCRIPTION, procIdModified)

        val eventSubscription = EventSubscription(
            name = "COMMENT_CREATE;ANY".toComposedEventName(),
            model = mapOf(
                "keyFoo" to "valueFoo",
                "keyBar" to "valueBar"
            ),
            predicate = Json.mapper.convert(
                """
                {
                    "att": "event.statusBefore",
                    "val": "approval",
                    "t": "eq"
                }
                """.trimIndent(),
                Predicate::class.java
            )!!
        )

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()

        assertThat(foundSubscription).hasSize(2)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            eventSubscription,
            eventSubscription.copy(
                model = mapOf(
                    "keyFoo" to "valueFoo",
                    "keyBar" to "valueBar2"
                )
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of signal boundary non inerrupting event with current document`() {
        val procId = "test-subscriptions-boundary-non-interrupting-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "COMMENT_CREATE;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of signal boundary event with current document`() {
        val procId = "test-subscriptions-boundary-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "COMMENT_CREATE;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of end signal event`() {
        val procId = "test-subscriptions-end-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "some-signal;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            )
        )
    }

    @Test
    fun `final all deployed subscriptions of throw signal event`() {
        val procId = "test-subscriptions-throw-signal-events"
        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()
        assertThat(foundSubscription).hasSize(1)

        assertThat(foundSubscription[0]).isEqualTo(
            EventSubscription(
                name = "some-signal;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of start signal events with event sub process`() {
        val procId = "test-subscriptions-start-signals-event-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()

        assertThat(foundSubscription).hasSize(2)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            EventSubscription(
                name = "start-1;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            ),
            EventSubscription(
                name = "start-2;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
                model = emptyMap(),
                predicate = null
            )
        )
    }

    @Test
    fun `find all deployed subscriptions of pool participant with hierarchy sub process`() {
        val procId = "test-subscriptions-pool-participants-with-hierarchy-subprocess-events"

        saveAndDeployBpmn(SUBSCRIPTION, procId)

        val foundSubscription = camundaEventSubscriptionFinder.findAllDeployedSubscription()
        val eventSubscription = EventSubscription(
            name = "signal-1;\${$VAR_BUSINESS_KEY}".toComposedEventName(),
            model = emptyMap(),
            predicate = null
        )

        assertThat(foundSubscription).hasSize(4)
        assertThat(foundSubscription).containsExactlyInAnyOrder(
            eventSubscription,
            eventSubscription.copy(name = "signal-2;ANY".toComposedEventName()),
            eventSubscription.copy(name = "signal-3;\${$VAR_BUSINESS_KEY}".toComposedEventName()),
            eventSubscription.copy(name = "signal-4;\${$VAR_BUSINESS_KEY}".toComposedEventName())
        )
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

private fun String.toComposedEventName(): ComposedEventName {
    return ComposedEventName.fromString(this)
}
