package ru.citeck.ecos.process.domain.bpmn

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.camunda.bpm.engine.*
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.init
import org.camunda.bpm.scenario.ProcessScenario
import org.camunda.bpm.scenario.Scenario
import org.camunda.bpm.scenario.Scenario.run
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.internal.verification.VerificationModeFactory.times
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.junit.jupiter.EnabledIf
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.config.lib.service.EcosConfigService
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.EmptyAuth
import ru.citeck.ecos.lazyapproval.model.MailProcessingCode
import ru.citeck.ecos.license.EcosTestLicense
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.notifications.lib.icalendar.CalendarEvent
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaMyBatisExtension
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.bpmn.event.BpmnEcosEventTestAction
import ru.citeck.ecos.process.domain.bpmn.event.SUBSCRIPTION
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiService
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmnla.services.BpmnLazyApprovalService
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDecisionLatestRecords
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

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
private const val BPMN_EVENTS = "bpmnevents"
private const val ERROR = "error"
private const val SERVICE_TASK = "servicetask"
private const val CONDITIONAL = "conditional"
private const val CALL_ACTIVITY = "callactivity"
private const val BUSINESS_KEY = "businesskey"

private const val FOLDER_LA = "bpmnla"

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
@SpringBootTest(classes = [EprocApp::class], properties = ["ecos-process.bpmn.elements.listener.enabled=true"])
class BpmnMonsterTestWithRunProcessTest {

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
    private lateinit var camundaMyBatisExtension: CamundaMyBatisExtension

    @Autowired
    private lateinit var procHistoricTaskService: ProcHistoricTaskService

    @Autowired
    private lateinit var bpmnEventHelper: BpmnEventHelper

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @SpyBean
    private lateinit var bpmnLazyApprovalService: BpmnLazyApprovalService

    @Mock
    private lateinit var process: ProcessScenario

    @Mock
    private lateinit var childProcess: ProcessScenario

    @SpyBean
    private lateinit var camundaRoleService: CamundaRoleService

    @MockBean
    private lateinit var statusSetter: CamundaStatusSetter

    @MockBean
    private lateinit var notificationService: NotificationService

    @MockBean
    private lateinit var bpmnKpiService: BpmnKpiService

    @MockBean
    private lateinit var ecosConfigService: EcosConfigService

    @SpyBean
    private lateinit var bpmnEcosEventTestAction: BpmnEcosEventTestAction

    private lateinit var documentRecordsDao: InMemRecordsDao<Any>

    private val kpiSettings = mutableListOf<EntityRef>()

    companion object {
        private const val KPI_ASYNC_WAIT_TIMEOUT_SECONDS = 15L

        private val harryRecord = PotterRecord()
        private val harryRef = EntityRef.valueOf("hogwarts/people@harry")

        private val ivanRecord = UserIvanRecord()
        private val ivanRef = EntityRef.valueOf("emodel/person@ivan")

        private val petyaRecord = UserPetyaRecord()
        private val petyaRef = EntityRef.valueOf("emodel/person@petya")

        private val germanRecord = UserGermanRecord()
        private val germanRef = EntityRef.valueOf("emodel/person@german")

        private val usersGroupRecord = UsersGroupRecord()
        private val usersGroupRef = EntityRef.valueOf("emodel/authority-group@users")

        private val docRecord = DocRecord()
        private val docRecord2 = DocRecord2()
        private val docRecord3 = DocRecord3()
        private val modifiedDocRecord = ModifiedDocRecord()
        private val docRef = EntityRef.valueOf("store/doc@1")
        private val docRef2 = EntityRef.valueOf("store/doc@2")
        private val docRef3 = EntityRef.valueOf("store/doc@3")
        private val docExplicitRef = EntityRef.valueOf("doc@explicit")

        private val variables_docRef = mapOf(
            BPMN_DOCUMENT_REF to docRef.toString()
        )

        private val mockRoleUserNames = listOf(USER_IVAN, USER_PETR)
        private val mockRoleGroupNames = listOf(GROUP_MANAGER, GROUP_DEVELOPER)
        private val mockRoleAuthorityNames = listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)

        private val mockAuthorEmails = listOf("$USER_IVAN@mail.com", "$USER_PETR@mail.com")
        private val mockInitiatorEmails = listOf("initiator@mail.com")
        private val mockApproverEmails = listOf("approver@mail.com")
        private val mockAuthorAccountantEmails = listOf("author@mail.com", "accountant@mail.com")
    }

    @BeforeEach
    fun setUp() {
        init(processEngine)

        recordsService.register(
            RecordsDaoBuilder.create("hogwarts/people")
                .addRecord(
                    harryRef.getLocalId(),
                    harryRecord
                )
                .build()
        )

        documentRecordsDao = RecordsDaoBuilder.create("store/doc")
            .addRecord(
                docRef.getLocalId(),
                docRecord
            )
            .addRecord(
                docRef2.getLocalId(),
                docRecord2
            )
            .addRecord(
                docRef3.getLocalId(),
                docRecord3
            )
            .build()

        recordsService.register(documentRecordsDao)

        recordsService.register(
            RecordsDaoBuilder.create("emodel/person")
                .addRecord(
                    ivanRef.getLocalId(),
                    ivanRecord
                )
                .addRecord(
                    petyaRef.getLocalId(),
                    petyaRecord
                )
                .addRecord(
                    germanRef.getLocalId(),
                    germanRecord
                )
                .build()
        )

        recordsService.register(
            RecordsDaoBuilder.create("emodel/authority-group")
                .addRecord(
                    usersGroupRef.getLocalId(),
                    usersGroupRecord
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
    fun clearSubscriptions() {
        helper.cleanDefinitions()
        camundaMyBatisExtension.deleteAllEventSubscriptions()
        recordsService.delete(kpiSettings)
    }

    // ---BPMN USER TASK TESTS ---

    @Test
    fun `complete simple task`() {
        val procId = "test-user-task-role"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task form ref check`() {
        val procId = "test-user-task-role"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasFormKey("uiserv/form@test-bpmn-form-task")
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task priority check`() {
        val procId = "test-user-task-priority"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it.priority).isEqualTo(TaskPriority.HIGH.toCamundaCode())
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task priority expression check`() {
        val procId = "test-user-task-priority-expression"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it.priority).isEqualTo(TaskPriority.LOW.toCamundaCode())
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "priority" to 3
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment by roles`() {
        val procId = "test-user-task-role"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            assertThat(it).hasCandidateGroup(GROUP_MANAGER)
            assertThat(it).hasCandidateGroup(GROUP_DEVELOPER)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task multi instance auto mode - assignment by roles`() {
        val procId = "test-user-task-role-multi-instance"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task multi instance parallel auto mode - assignment by roles`() {
        val procId = "test-user-task-role-multi-instance-parallel"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance collection`() {
        val procId = "test-user-task-manual-collection-multi-instance"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
        ).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance parallel collection`() {
        val procId = "test-user-task-manual-collection-multi-instance-parallel"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
        ).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance loop`() {
        val procId = "test-user-task-manual-loop-multi-instance"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        run(process).startByKey(procId).engine(processEngine).execute()

        verify(process, times(4)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual multi instance loop parallel`() {
        val procId = "test-user-task-manual-loop-multi-instance-parallel"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        run(process).startByKey(procId).engine(processEngine).execute()

        verify(process, times(4)).hasCompleted("userTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual`() {
        val procId = "test-user-task-assign-manual"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            assertThat(it).hasCandidateGroup(GROUP_MANAGER)
            assertThat(it).hasCandidateGroup(GROUP_DEVELOPER)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual with expressions`() {
        val procId = "test-user-task-assign-manual-with-expressions"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to docRef.toString(),
                "recipientsFromIncomeProcessVariables" to listOf("userFromVariable", "GROUP_fromVariable")
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task assignment manual single recipient with expressions`() {
        val procId = "test-user-task-assign-single-manual-with-expressions"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser("userFromVariable")

            assertThat(it).hasCandidateGroup("GROUP_fromVariable")

            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "recipientsFromIncomeProcessVariables" to listOf("userFromVariable", "GROUP_fromVariable")
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task ml text name`() {
        val procId = "test-user-task-ml-name"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task ml text name single lang`() {
        val procId = "test-user-task-ml-name-single-lang"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task empty json ml text should return default name`() {
        val procId = "test-user-task-ml-empty-json"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task blank ml text should return default name`() {
        val procId = "test-user-task-ml-blank"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task without json ml text should return default name`() {
        val procId = "test-user-task-without-ml-json"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task due date explicit text`() {
        val procId = "test-user-task-due-date-explicit"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        val expectedFollowUpDate = Instant.parse("2023-02-07T15:00:00.0Z")
        val expectedDueDate = Instant.parse("2023-02-07T21:30:00.0Z")

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(
                ZonedDateTime.ofInstant(it.followUpDate.toInstant(), ZoneOffset.UTC).toInstant()
            ).isEqualTo(
                expectedFollowUpDate
            )

            assertThat(
                ZonedDateTime.ofInstant(it.dueDate.toInstant(), ZoneOffset.UTC).toInstant()
            ).isEqualTo(
                expectedDueDate
            )

            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `task due date from variables`() {
        val procId = "test-user-task-due-date-from-variables"
        helper.saveAndDeployBpmn(USER_TASK, procId)

        val expectedFollowUpDate = Instant.parse("2023-02-07T15:00:00.0Z")
        val expectedDueDate = Instant.parse("2023-02-07T21:30:00.0Z")

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(
                ZonedDateTime.ofInstant(it.followUpDate.toInstant(), ZoneOffset.UTC).toInstant()
            ).isEqualTo(
                expectedFollowUpDate
            )

            assertThat(
                ZonedDateTime.ofInstant(it.dueDate.toInstant(), ZoneOffset.UTC).toInstant()
            ).isEqualTo(
                expectedDueDate
            )

            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    // ---BPMN SCRIPT TASK TESTS ---

    @Test
    fun `script task set variables`() {
        val procId = "test-script-task-set-variables"
        helper.saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "foo" to "foo"
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("fooBar", "foo bar")
        assertThat(scenario.instance(process)).variables().containsEntry("newVariable", "new var from script")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `script task can running without document ref`() {
        val procId = "test-script-task-set-variables"
        helper.saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "foo" to "foo"
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("fooBar", "foo bar")
        assertThat(scenario.instance(process)).variables().containsEntry("newVariable", "new var from script")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `script task get document variables`() {
        val procId = "test-script-task-get-document-variables"
        helper.saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to harryRef.toString()
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("loadedName", harryRecord.name)
        assertThat(scenario.instance(process)).variables().containsEntry("loadedEmail", harryRecord.email)

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `script task check role service`() {
        val procId = "test-script-task-check-role-service"
        helper.saveAndDeployBpmn("scripttask", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to harryRef.toString()
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("userNames", mockRoleUserNames)
        assertThat(scenario.instance(process)).variables().containsEntry("groupNames", mockRoleGroupNames)
        assertThat(scenario.instance(process)).variables().containsEntry("authorityNames", mockRoleAuthorityNames)

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `script task should prevent save js date`() {
        val procId = "test-save-js-date-script"
        helper.saveAndDeployBpmn("scripttask", procId)

        assertThrows<Exception> {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }
    }

    @Test
    fun `script task should prevent save js object`() {
        val procId = "test-save-js-object-script"
        helper.saveAndDeployBpmn("scripttask", procId)

        assertThrows<Exception> {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }
    }

    // ---BPMN GATEWAY TESTS ---

    @Test
    fun `gateway condition javascript compare variable variant top`() {
        val procId = "test-gateway-condition-javascript"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "top",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endTop")
    }

    @Test
    fun `gateway condition javascript compare variable variant bottom`() {
        val procId = "test-gateway-condition-javascript"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "bottom",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endBottom")
    }

    @Test
    fun `gateway condition javascript none of the conditions are true, should go to default flow`() {
        val procId = "test-gateway-condition-javascript"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "no-one-flow",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endDefault")
    }

    @Test
    fun `gateway condition expression compare variable variant top`() {
        val procId = "test-gateway-condition-expression"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "top",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endTop")
    }

    @Test
    fun `gateway condition expression compare variable variant bottom`() {
        val procId = "test-gateway-condition-expression"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "bottom",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endBottom")
    }

    @Test
    fun `gateway condition expression none of the conditions are true, should go to default flow`() {
        val procId = "test-gateway-condition-expression"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "flow" to "no-one-flow",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endDefault")
    }

    @Test
    fun `gateway condition task outcome variant done`() {
        val procId = "test-gateway-condition-task-outcome"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "done", MLText.EMPTY)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                taskOutcome.outcomeId() to taskOutcome.value,
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endDone")
    }

    @Test
    fun `gateway condition task outcome variant cancel`() {
        val procId = "test-gateway-condition-task-outcome"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "cancel", MLText.EMPTY)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                taskOutcome.outcomeId() to taskOutcome.value,
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endCancel")
    }

    @Test
    fun `gateway condition task outcome, complete task with incorrect outcome should throw exception`() {
        val procId = "test-gateway-condition-task-outcome"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        val taskOutcome = Outcome("userTask", "incorrect_task_outcome", MLText.EMPTY)

        assertThrows<ProcessEngineException> {
            `when`(process.waitsAtUserTask("userTask")).thenReturn {
                it.complete()
            }

            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString(),
                    taskOutcome.outcomeId() to taskOutcome.value,
                )
            ).engine(processEngine).execute()
        }
    }

    @Test
    fun `gateway inclusive gateway all flows`() {
        val procId = "test-inclusive-gateway"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "foo" to "bar",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_all_event")
        verify(process).hasFinished("end_condition_event")
    }

    @Test
    fun `gateway inclusive only all event flow`() {
        val procId = "test-inclusive-gateway"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "foo" to "not_bar",
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_all_event")
        verify(process, never()).hasFinished("end_condition_event")
    }

    @Test
    fun `gateway event based first variant`() {
        val procId = "test-event-based-gateway"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        `when`(process.waitsAtEventBasedGateway("event_based_gateway")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-gateway-signal-1",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("Event_first")
        verify(process, never()).hasFinished("Event_second")
        verify(process).hasFinished("end_event")
    }

    @Test
    fun `gateway event based second variant`() {
        val procId = "test-event-based-gateway"
        helper.saveAndDeployBpmn(GATEWAY, procId)

        `when`(process.waitsAtEventBasedGateway("event_based_gateway")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-gateway-signal-2",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("Event_second")
        verify(process, never()).hasFinished("Event_first")
        verify(process).hasFinished("end_event")
    }

    // --- BPMN SET STATUS TESTS ---

    @Test
    fun `set status of document`() {
        val procId = "test-set-status"
        helper.saveAndDeployBpmn("status", procId)

        val docRef = EntityRef.valueOf(docRef.toString())

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString()
            )
        ).engine(processEngine).execute()

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
        helper.saveAndDeployBpmn(SUB_PROCESS, procId)

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
                BPMN_DOCUMENT_REF to docRef.toString()
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `sub process multi instance sequential`() {
        val procId = "test-sub-process-multi-instance-sequential"
        helper.saveAndDeployBpmn(SUB_PROCESS, procId)

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
                BPMN_DOCUMENT_REF to docRef.toString(),
                "testCandidateCollection" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)
            )
        ).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process, times(3)).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `sub process multi instance parallel`() {
        val procId = "test-sub-process-multi-instance-parallel"
        helper.saveAndDeployBpmn(SUB_PROCESS, procId)

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
                BPMN_DOCUMENT_REF to docRef.toString(),
                "testCandidateCollection" to listOf(USER_IVAN, USER_PETR, GROUP_MANAGER)
            )
        ).engine(processEngine).execute()

        verify(process, times(3)).hasCompleted("userTask")
        verify(process, times(3)).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    // --- BPMN SEND TASK TESTS ---

    @Test
    fun `send task with template`() {
        val procId = "test-send-task-with-template"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(EntityRef.valueOf("notifications/template@test-template"))
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .title("Hello Ivan")
            .body("<p>Hello Ivan, your document is approved</p>")
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
    fun `send task role recipients check`() {
        val procId = "test-send-task-recipients-check"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorAccountantEmails)
            .cc(mockInitiatorEmails)
            .bcc(mockApproverEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(EntityRef.valueOf("notifications/template@test-template"))
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
    fun `send task role recipients roles with expression check`() {
        val procId = "test-send-task-recipients-roles-with-expressions-check"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .recipients(mockAuthorAccountantEmails + "exp-recipient@mail.ru")
            .cc(mockInitiatorEmails + "exp-cc@mail.ru")
            .bcc(mockApproverEmails + "exp-bcc@mail.ru")
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(EntityRef.valueOf("notifications/template@test-template"))
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
    fun `send task without document check`() {
        val procId = "test-send-task-without-document-check"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                emptyMap()
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(EntityRef.EMPTY)
            .recipients(listOf("someMail@mail.ru"))
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .templateRef(EntityRef.valueOf("notifications/template@test-template"))
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
    fun `send task expression recipients check`() {
        val procId = "test-send-task-recipients-expression"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                emptyMap()
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(EntityRef.EMPTY)
            .recipients(listOf("mailfromvariable@mail.ru", "1@mail.ru", "2@mail.ru", "petya@mail.ru"))
            .cc(listOf("ccivan@mail.ru"))
            .bcc(listOf("bcc@mail.ru"))
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .title("123")
            .body("<p>456</p>")
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        "currentRunAsUser" to EntityRef.EMPTY,
                        "fromVariable" to "mailfromvariable@mail.ru",
                        "fromVariable2" to "1@mail.ru,2@mail.ru"
                    )
                )
            )
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
    fun `send task expression user and group recipients check`() {
        val procId = "test-send-task-recipients-expression-user-group"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                emptyMap()
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(EntityRef.EMPTY)
            .recipients(listOf("mailfromvariable@mail.ru", "ivan@mail.ru", "petya@mail.ru", "some@mail.ru"))
            .cc(listOf("ivan@mail.ru", "petya@mail.ru", "some@mail.ru"))
            .bcc(listOf("ivan@mail.ru", "petya@mail.ru", "some@mail.ru"))
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .title("123")
            .body("<p>456</p>")
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        "fromVariableGroup" to "GROUP_users",
                        "fromVariableUser" to "ivan",
                        "fromVariable" to "mailfromvariable@mail.ru",
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
    fun `send task one line expression user and group recipients check`() {
        val procId = "test-send-task-recipients-one-expression-user-group"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                emptyMap()
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(EntityRef.EMPTY)
            .recipients(listOf("mailfromvariable@mail.ru", "ivan@mail.ru", "petya@mail.ru", "some@mail.ru"))
            .cc(listOf("mailfromvariable@mail.ru", "ivan@mail.ru", "petya@mail.ru", "some@mail.ru"))
            .bcc(listOf("ivan@mail.ru", "petya@mail.ru"))
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("ru")
            .title("123")
            .body("<p>456</p>")
            .additionalMeta(
                mapOf(
                    "process" to mapOf(
                        "fromVariableGroup" to "GROUP_users",
                        "fromVariableUser" to "ivan",
                        "fromVariable" to "mailfromvariable@mail.ru",
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
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
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docExplicitRef)
            .recipients(mockAuthorEmails)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .lang("en")
            .additionalMeta(
                mapOf(
                    "foo" to EntityRef.valueOf("bar"),
                    "harry" to EntityRef.valueOf("potter"),
                    "process" to mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        "currentRunAsUser" to EntityRef.EMPTY
                    )
                )
            )
            .templateRef(EntityRef.valueOf("notifications/template@test-template"))
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
    fun `send task with send calendar event`() {
        val procId = "test-send-task-with-send-calendar-event"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                emptyMap()
            ).engine(processEngine).execute()
        }

        val calendarEvent = CalendarEvent.Builder("Summary", Instant.parse("2024-07-31T12:00:00Z"))
            .uid("2b60f57d-11da-48be-b4cd-7b9a206a6477")
            .description("Description")
            .durationInMillis(Duration.parse("PT2H").toMillis())
            .organizer("organizer@mail.ru")
            .attendees(listOf("1@mail.ru", "2@mail.ru"))
            .createDate(Instant.parse("2024-07-29T00:00:00Z"))
            .build()
        val attachment = calendarEvent.createAttachment()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                val event = it.additionalMeta["_attachments"] as CalendarEvent.CalendarEventAttachment
                val eventFileText = Base64.getMimeDecoder().decode(event.bytes)
                val updatedEventText =
                    eventFileText.decodeToString().replace("DTSTAMP:.+Z".toRegex(), "DTSTAMP:20240729T000000Z")

                assertThat(updatedEventText).isEqualTo(
                    Base64.getMimeDecoder().decode(attachment.bytes).decodeToString()
                )
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `send task with send calendar event values`() {
        val procId = "test-send-task-with-send-calendar-event-values"
        helper.saveAndDeployBpmn(SEND_TASK, procId)

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val calendarEvent = CalendarEvent.Builder("Test Name", Instant.parse("2024-12-17T09:00:00Z"))
            .uid("2b60f57d-11da-48be-b4cd-7b9a206a6477")
            .description("Test Description")
            .durationInMillis(Duration.parse("PT1H").toMillis())
            .organizer(mockAuthorEmails.first())
            .attendees(listOf("1@mail.ru", "2@mail.ru"))
            .createDate(Instant.parse("2024-12-16T00:00:00Z"))
            .build()
        val attachment = calendarEvent.createAttachment()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                val event = it.additionalMeta["_attachments"] as CalendarEvent.CalendarEventAttachment
                val eventFileText = Base64.getMimeDecoder().decode(event.bytes)
                val updatedEventText =
                    eventFileText.decodeToString().replace("DTSTAMP:.+Z".toRegex(), "DTSTAMP:20241216T000000Z")

                assertThat(updatedEventText).isEqualTo(
                    Base64.getMimeDecoder().decode(attachment.bytes).decodeToString()
                )
            }
        )

        verify(process, times(1)).hasCompleted("sendTask")
        verify(process).hasFinished("endEvent")
    }

    // --- BPMN TIMER TESTS ---

    @Test
    fun `timer boundary non interrupting`() {
        val procId = "test-timer-boundary-non-interrupting"
        helper.saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P2DT12H") { task.complete() }
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, times(2)).hasFinished("remindSendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `timer value from expression`() {
        val procId = "test-timer-value-from-expression"
        helper.saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P4DT12H") { task.complete() }
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                "timeValue" to "R/P1D"
            )
        ).engine(processEngine).execute()

        verify(process, times(4)).hasFinished("remindSendTask")
        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `timer boundary duration interrupting - done task flow`() {
        val procId = "test-timer-boundary-duration-interrupting"
        helper.saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("taskDoneFlow")
        verify(process, never()).hasFinished("taskExpiredFlow")
    }

    @Test
    fun `timer boundary duration interrupting - expired task flow`() {
        val procId = "test-timer-boundary-duration-interrupting"
        helper.saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            task.defer("P3DT12H") { }
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        assertThat(getActiveTasksCountForProcess(procId)).isEqualTo(0)

        verify(process).hasCompleted("expiredSendTask")
        verify(process).hasFinished("taskExpiredFlow")
        verify(process, never()).hasFinished("taskDoneFlow")
    }

    @Test
    fun `timer intermediate catch`() {
        val procId = "test-timer-intermediate-catch"
        helper.saveAndDeployBpmn(TIMER, procId)

        `when`(process.waitsAtTimerIntermediateEvent("timer")).thenReturn {
            // Do nothing means process moves forward because of the timers
        }
        `when`(process.waitsAtUserTask("userTask")).thenReturn { task ->
            task.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    // --- BPMN HISTORIC TASKS TESTS ---

    @Test
    fun `historic tasks order validation`() {
        val procId = "test-user-task-role-multi-instance"
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        val scenario = run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

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
        helper.saveAndDeployBpmn(USER_TASK, procId)

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

        val scenario = run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

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
        helper.saveAndDeployBpmn(POOL, procId)

        `when`(process.waitsAtUserTask("userTask_1")).thenReturn {
            it.complete()
        }

        `when`(process.waitsAtUserTask("userTask_2")).thenReturn {
            it.complete()
        }

        val scenario = run(process).startByKey(
            procId,
            variables_docRef
        ).engine(processEngine).execute()

        verify(process, times(1)).hasCompleted("userTask_1")
        verify(process, times(1)).hasCompleted("userTask_2")
        verify(process, times(1)).hasCompleted("scriptTask")

        assertThat(scenario.instance(process)).variables().containsEntry("foo", "bar")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `pool with child lanes set`() {
        val procId = "test-child-lanes"
        helper.saveAndDeployBpmn(POOL, procId)

        run(process).startByKey(
            procId,
            variables_docRef
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_event")
    }

    // --- CamundaEventSubscriptionFinder TESTS ---
    @Test
    fun `get actual camunda subscriptions of 2 different process with start event`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdModified = "test-subscriptions-start-signal-event_2"

        val subscriptions = getSubscriptionsAfterAction(
            IncomingEventData(eventName = "ecos.comment.create")
        ) {
            helper.saveAndDeployBpmn(SUBSCRIPTION, procId)
            helper.saveAndDeployBpmn(SUBSCRIPTION, procIdModified)
        }

        val eventSubscription = EventSubscription(
            elementId = "startEvent",
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name
            ),
            model = mapOf(
                "keyFoo" to "valueFoo",
                "keyBar" to "valueBar"
            ),
            predicate = """
                {
                    "att": "event.statusBefore",
                    "val": "approval",
                    "t": "eq"
                }
            """.trimIndent()
        )

        assertThat(subscriptions).hasSize(2)
        assertThat(subscriptions).containsExactlyInAnyOrder(
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
    fun `get actual camunda subscriptions of 2 versions process with start event - start event should be last version`() {
        val procId = "test-subscriptions-start-signal-event"
        val procIdVersion2 = "test-subscriptions-start-signal-event-version_2"

        val subscriptions = getSubscriptionsAfterAction(
            IncomingEventData(eventName = "ecos.comment.create")
        ) {
            helper.saveAndDeployBpmn(SUBSCRIPTION, procId)
            helper.saveAndDeployBpmn(SUBSCRIPTION, procIdVersion2)
        }

        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions).containsExactlyInAnyOrder(
            EventSubscription(
                elementId = "startEvent",
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name
                ),
                model = mapOf(
                    "keyFoo2" to "valueFoo2",
                    "keyBar" to "valueBar"
                ),
                predicate = """
                {
                    "att": "event.statusBefore",
                    "val": "approval2",
                    "t": "eq"
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `get actual camunda subscriptions of boundary event`() {
        val procId = "test-subscriptions-event-boundary"
        val document = docRef.toString()

        helper.saveAndDeployBpmn(SUBSCRIPTION, procId)

        val existingSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            IncomingEventData(
                eventName = "ecos.comment.create",
                record = EntityRef.valueOf(document)
            )
        ).map { it.event }

        `when`(process.waitsAtUserTask("approverTask")).thenReturn { task ->
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "ecos.comment.create",
                    record = EntityRef.valueOf(document)
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(1)
            assertThat(diff).containsExactlyInAnyOrder(
                EventSubscription(
                    elementId = "event_create_comment",
                    name = ComposedEventName(
                        event = EcosEventType.COMMENT_CREATE.name,
                        record = "\${$BPMN_BUSINESS_KEY}"
                    ),
                    model = mapOf(
                        "foo" to "bar",
                        "key" to "value"
                    ),
                    predicate = """
                {
                    "att": "event.statusBefore",
                    "val": "approval",
                    "t": "eq"
                }
                    """.trimIndent()
                )
            )

            task.complete()
        }

        run(process).startByKey(
            procId,
            document,
            mapOf(
                BPMN_DOCUMENT_REF to document
            )
        ).engine(processEngine).execute()

        val subscriptionWhenTaskComplete = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            IncomingEventData(
                eventName = "ecos.comment.create",
                record = EntityRef.valueOf(document)
            )
        ).map { it.event }

        assertThat(subscriptionWhenTaskComplete).hasSize(existingSubscriptions.size)

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `get actual camunda subscriptions of boundary event parallel process`() {
        val procId = "test-subscriptions-event-boundary-parallel"
        val document = docRef.toString()

        helper.saveAndDeployBpmn(SUBSCRIPTION, procId)

        val existingSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            IncomingEventData(
                eventName = "ecos.comment.create",
                record = EntityRef.valueOf(document)
            )
        ).map { it.event }

        `when`(process.waitsAtUserTask("task_1")).thenReturn {
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "ecos.comment.create",
                    record = EntityRef.valueOf(document)
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(1)
            assertThat(diff).containsExactlyInAnyOrder(
                EventSubscription(
                    elementId = "event_create_comment",
                    name = ComposedEventName(
                        event = EcosEventType.COMMENT_CREATE.name,
                        record = "\${$BPMN_BUSINESS_KEY}"
                    ),
                    model = mapOf(
                        "foo" to "bar",
                        "key" to "value"
                    ),
                    predicate = """
                    {
                    "att": "event.statusBefore",
                    "val": "approval",
                    "t": "eq"
                    }
                    """.trimIndent()
                )
            )
        }

        `when`(process.waitsAtUserTask("task_2")).thenReturn {
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "manual-event",
                    record = EntityRef.valueOf(document),
                    recordType = EntityRef.valueOf("emodel/type@hr-person")
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(1)
            assertThat(diff).containsExactlyInAnyOrder(
                EventSubscription(
                    elementId = "event_manual",
                    name = ComposedEventName(
                        event = "manual-event",
                        record = ComposedEventName.RECORD_ANY,
                        type = "emodel/type@hr-person"
                    ),
                    model = emptyMap()
                )
            )
        }

        run(process).startByKey(
            procId,
            document,
            mapOf(
                BPMN_DOCUMENT_REF to document
            )
        ).engine(processEngine).execute()
    }

    @Test
    fun `get actual camunda subscriptions of boundary same events parallel process`() {
        val procId = "test-subscriptions-event-boundary-parallel-with-same-events"
        val document = docRef.toString()

        helper.saveAndDeployBpmn(SUBSCRIPTION, procId)

        val existingSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            IncomingEventData(
                eventName = "ecos.comment.create",
                record = EntityRef.valueOf(document)
            )
        ).map { it.event }

        val createCommentEvent = EventSubscription(
            elementId = "event_create_comment",
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name,
                record = "\${$BPMN_BUSINESS_KEY}"
            ),
            model = emptyMap()
        )
        val createCommentEventSecond = createCommentEvent.copy(
            elementId = "event_create_comment_2"
        )

        `when`(process.waitsAtUserTask("task_1")).thenReturn {
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "ecos.comment.create",
                    record = EntityRef.valueOf(document)
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(2)
            assertThat(diff).containsExactlyInAnyOrder(
                createCommentEvent,
                createCommentEventSecond
            )
        }

        `when`(process.waitsAtUserTask("task_2")).thenReturn {
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "ecos.comment.create",
                    record = EntityRef.valueOf(document)
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(2)
            assertThat(diff).containsExactlyInAnyOrder(
                createCommentEvent,
                createCommentEventSecond
            )
        }

        run(process).startByKey(
            procId,
            document,
            mapOf(
                BPMN_DOCUMENT_REF to document
            )
        ).engine(processEngine).execute()
    }

    @Test
    fun `get actual camunda subscriptions of multiple start signals with diff predicate process`() {
        val procId = "test-subscriptions-multiple-start-signals-with-diff-predicate"
        val document = docRef.toString()

        helper.saveAndDeployBpmn(SUBSCRIPTION, procId)

        val existingSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            IncomingEventData(
                eventName = "ecos.comment.create",
                record = EntityRef.valueOf(document)
            )
        ).map { it.event }

        val eventCommentFirst = EventSubscription(
            elementId = "event_1",
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name,
                record = "\${$BPMN_BUSINESS_KEY}"
            ),
            model = emptyMap(),
            predicate = """
                {
                    "att": "text",
                    "val": "comment_1",
                    "t": "eq"
                }
            """.trimIndent()
        )

        val eventCommentSecond = EventSubscription(
            elementId = "event_2",
            name = ComposedEventName(
                event = EcosEventType.COMMENT_CREATE.name,
                record = "\${$BPMN_BUSINESS_KEY}"
            ),
            model = emptyMap(),
            predicate = """
                {
                    "att": "text",
                    "val": "comment_2",
                    "t": "eq"
                }
            """.trimIndent()
        )

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            val subscriptionWhenTaskRun = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
                IncomingEventData(
                    eventName = "ecos.comment.create",
                    record = EntityRef.valueOf(document)
                )
            ).map { it.event }

            val diff = ListUtils.subtract(subscriptionWhenTaskRun, existingSubscriptions)

            assertThat(diff).hasSize(2)
            assertThat(diff).containsExactlyInAnyOrder(
                eventCommentFirst,
                eventCommentSecond
            )
        }

        run(process).startByKey(
            procId,
            document,
            mapOf(
                BPMN_DOCUMENT_REF to document
            )
        ).engine(processEngine).execute()
    }

    // --- BPMN EVENTS TESTS ---

    @Test
    fun `bpmn event manual current document`() {
        val procId = "bpmn-events-manual-current-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual current document must not call while document not match`() {
        val procId = "bpmn-events-manual-current-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to "another-document"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual current document must not call while event name not match`() {
        val procId = "bpmn-events-manual-current-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test-another",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual any document`() {
        val procId = "bpmn-events-manual-any-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = emptyMap()
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual any document must not call while event name not match`() {
        val procId = "bpmn-events-manual-any-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test-another",
                eventData = emptyMap()
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual filter by type`() {
        val procId = "bpmn-events-manual-filter-by-type-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to docRef
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual filter by type must not call while type not match`() {
        val procId = "bpmn-events-manual-filter-by-type-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to harryRef
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual filter by document from variable`() {
        val procId = "bpmn-events-manual-filter-by-document-from-variable-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val filterDocument = docRef.toString()

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to filterDocument
                )
            )
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to harryRef.toString(),
                "docForFilter" to filterDocument
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event manual filter by document from variable must not call while document not match`() {
        val procId = "bpmn-events-manual-filter-by-document-from-variable-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val filterDocument = docRef.toString()

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "event-int-catch-test",
                eventData = mapOf(
                    "record" to filterDocument + "another"
                )
            )
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to harryRef.toString(),
                "docForFilter" to filterDocument
            )
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event predefined`() {
        val procId = "bpmn-events-predefined-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event predefined must not call while event is not match`() {
        val procId = "bpmn-events-predefined-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendUpdateCommentEvent(
                CommentUpdateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    textBefore = "text comment before",
                    textAfter = "test comment after"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event predefined attributes test`() {
        val procId = "bpmn-events-predefined-variables-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val commentRecord = EntityRef.valueOf("eproc/comment@1")
        val commentText = "test comment"

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = commentRecord,
                    text = commentText
                )
            )
        }

        val scenario = run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["text"].asText()
        }.isEqualTo(commentText)

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["commentRecord"].asText()
        }.isEqualTo(commentRecord.toString())

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["record"].asText()
        }.isEqualTo(docRef.toString())

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["recordType"].asText()
        }.isEqualTo(docRecord.type.toString())
    }

    @Test
    fun `bpmn event meta attributes test`() {
        val procId = "bpmn-events-predefined-variables-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val commentRecord = EntityRef.valueOf("eproc/comment@1")
        val commentText = "test comment"

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = commentRecord,
                    text = commentText
                )
            )
        }

        val scenario = run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it[EVENT_META_ATT][EVENT_META_ID_ATT].asText()
        }.isNotNull

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it[EVENT_META_ATT][EVENT_META_TIME_ATT].asText()
        }.isNotNull

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it[EVENT_META_ATT][EVENT_META_TYPE_ATT].asText()
        }.isEqualTo(CommentCreateEvent.TYPE)

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it[EVENT_META_ATT][EVENT_META_USER_ATT].asText()
        }.isEqualTo(TEST_USER)
    }

    @Test
    fun `bpmn event user model attributes test`() {
        val procId = "bpmn-events-predefined-user-model-variables-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val commentRecord = EntityRef.valueOf("eproc/comment@1")
        val commentText = "test comment"

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = harryRef,
                    commentRecord = commentRecord,
                    text = commentText
                )
            )
        }

        val scenario = run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["harryName"].asText()
        }.isEqualTo(harryRecord.name)

        assertThat(scenario.instance(process)).variables().extracting("event").extracting {
            it as BpmnDataValue
            it["harryEmail"].asText()
        }.isEqualTo(harryRecord.email)
    }

    @Test
    fun `bpmn event user model variables predicate`() {
        val procId = "bpmn-events-user-model-variables-predicate-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = harryRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event user model variables predicate not match`() {
        val procId = "bpmn-events-user-model-variables-predicate-false-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = harryRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event predefined model variables predicate`() {
        val procId = "bpmn-events-predefined-variables-predicate-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event predefined model variables predicate not match`() {
        val procId = "bpmn-events-predefined-variables-predicate-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "not match"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event meta variables predicate`() {
        val procId = "bpmn-events-meta-variables-predicate-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event meta variables predicate not match`() {
        val procId = "bpmn-events-meta-variables-predicate-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                ),
                "notMatchUser"
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event user model variables predicate with expression`() {
        val procId = "bpmn-events-user-model-variables-predicate-with-expression-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = docRecord.name
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event intermediate catch`() {
        val procId = "bpmn-events-intermediate-catch-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("event_catch")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event boundary non interrupting`() {
        val procId = "bpmn-events-boundary-non-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("task")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("boundaryEvent")
        verify(process).hasFinished("endEventFromBoundary")
        verify(process, never()).hasCanceled("task")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event boundary non interrupting multiple times`() {
        val procId = "bpmn-events-boundary-non-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val repeatCount = 3

        `when`(process.waitsAtUserTask("task")).thenReturn {
            repeat(repeatCount) {
                bpmnEventHelper.sendCreateCommentEvent(
                    CommentCreateEvent(
                        record = docRef,
                        commentRecord = EntityRef.valueOf("eproc/comment@1"),
                        text = "test comment"
                    )
                )
            }
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, times(repeatCount)).hasFinished("boundaryEvent")
        verify(process, times(repeatCount)).hasFinished("endEventFromBoundary")
        verify(process, never()).hasCanceled("task")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event boundary interrupting`() {
        val procId = "bpmn-events-boundary-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("task")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("boundaryEvent")
        verify(process).hasFinished("endEventFromBoundary")
        verify(process).hasCanceled("task")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event start non interrupting`() {
        val procId = "bpmn-events-start-non-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event start non interrupting multiple times`() {
        val procId = "bpmn-events-start-non-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        val repeatCount = 3

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            repeat(repeatCount) {
                bpmnEventHelper.sendCreateCommentEvent(
                    CommentCreateEvent(
                        record = docRef,
                        commentRecord = EntityRef.valueOf("eproc/comment@1"),
                        text = "test comment"
                    )
                )
            }
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, times(repeatCount)).hasFinished("startEvent")
        verify(process, times(repeatCount)).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event start interrupting`() {
        val procId = "bpmn-events-start-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "test comment"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event start interrupting multiple times`() {
        val procId = "bpmn-events-start-interrupting-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            repeat(3) {
                bpmnEventHelper.sendCreateCommentEvent(
                    CommentCreateEvent(
                        record = docRef,
                        commentRecord = EntityRef.valueOf("eproc/comment@1"),
                        text = "test comment"
                    )
                )
            }
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process, times(1)).hasFinished("startEvent")
        verify(process, times(1)).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event start same events with different predicates no one calls`() {
        val procId = "bpmn-events-start-same-with-diff-predicates-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "another comment"
                )
            )
            it.complete()
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("event_main_end")
        verify(process, never()).hasFinished("event_end_1")
        verify(process, never()).hasFinished("event_end_2")
    }

    @Test
    fun `bpmn event start same events with different predicates first path`() {
        val procId = "bpmn-events-start-same-with-diff-predicates-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "comment_1"
                )
            )
            it.complete()
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("event_main_end")
        verify(process).hasFinished("event_end_1")
        verify(process, never()).hasFinished("event_end_2")
        verify(process, never()).hasFinished("event_end_3")
    }

    @Test
    fun `bpmn event start same events with different predicates second path`() {
        val procId = "bpmn-events-start-same-with-diff-predicates-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "comment_2"
                )
            )
            it.complete()
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("event_main_end")
        verify(process).hasFinished("event_end_2")
        verify(process, never()).hasFinished("event_end_1")
        verify(process, never()).hasFinished("event_end_3")
    }

    @Test
    fun `bpmn event start same events with different predicates third path`() {
        val procId = "bpmn-events-start-same-with-diff-predicates-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            bpmnEventHelper.sendCreateCommentEvent(
                CommentCreateEvent(
                    record = docRef,
                    commentRecord = EntityRef.valueOf("eproc/comment@1"),
                    text = "comment_3"
                )
            )
            it.complete()
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("event_main_end")
        verify(process).hasFinished("event_end_3")
        verify(process, never()).hasFinished("event_end_1")
        verify(process, never()).hasFinished("event_end_2")
    }

    @Test
    fun `bpmn event throw ecos event from script task`() {
        val procId = "bpmn-events-ecos-event-script-task-throw-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            // do nothing
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("Script_task_throw")
        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event throw ecos event from script task payload test`() {
        val procId = "bpmn-events-ecos-event-script-task-throw-payload-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEventBase")
        verify(bpmnEcosEventTestAction, Mockito.times(1)).callAction(
            org.mockito.kotlin.check {
                assertThat(it["foo"].asText()).isEqualTo("bar")
                assertThat(it["itsNum"].asInt()).isEqualTo(123)
            }
        )
    }

    @Test
    fun `bpmn event intermediate throw`() {
        val procId = "bpmn-events-intermediate-throw-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            // do nothing
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("eventThrow")
        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
        verify(process, never()).hasFinished("endEventBase")
    }

    @Test
    fun `bpmn event end throw`() {
        val procId = "bpmn-events-end-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endThrow")
        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
    }

    @Test
    fun `bpmn event end throw to start event - with filter by current document`() {
        val procId = "bpmn-events-end-with-current-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endThrow")
        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
    }

    @Test
    fun `bpmn event end throw to start event - with filter by document type`() {
        val procId = "bpmn-events-end-with-filter-by-document-type-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endThrow")
        verify(process).hasFinished("startEvent")
        verify(process).hasFinished("endEventFromStart")
    }

    @Test
    fun `bpmn event end throw to start event - with filter by document type not match`() {
        val procId = "bpmn-events-end-with-filter-by-document-type-false-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endThrow")
        verify(process, never()).hasFinished("startEvent")
        verify(process, never()).hasFinished("endEventFromStart")
    }

    @Test
    fun `bpmn event user event current document`() {
        val procId = "bpmn-events-user-event-current-document-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "user-event-action1",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event status manual current document`() {
        val procId = "bpmn-events-status-manual-test"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtSignalIntermediateCatchEvent("signal_catch")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "record-status-changed",
                eventData = mapOf(
                    "record" to docRef.toString(),
                    "before" to "on_task",
                    "after" to "new"
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `bpmn event sub process with user task signal`() {
        val procId = "bpmn-events-sub-process-user-task-signal-event"
        helper.saveAndDeployBpmn(BPMN_EVENTS, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it).hasCandidateUser(USER_IVAN)
            assertThat(it).hasCandidateUser(USER_PETR)

            it.complete()
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endSubProcess")
        verify(process).hasFinished("endEvent")
    }

    // --- BPMN SERVICES TESTS ---

    @Test
    fun `bpmn complete all active tasks with default outcome`() {
        val procId = "test-complete-all-tasks"
        helper.saveAndDeployBpmnFromResource("test/bpmn/services/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("task_1")).thenReturn {
            bpmnEventHelper.sendManualEvent(
                eventName = "complete-tasks-default",
                eventData = mapOf(
                    "record" to docRef.toString()
                )
            )
        }

        run(process).startByKey(procId, docRef.toString(), variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("task_1")
        verify(process).hasFinished("task_2")
        verify(process).hasFinished("task_3")

        verify(process).hasFinished("end_all")

        verify(process, never()).hasFinished("end_default_1")
        verify(process, never()).hasFinished("end_default_3")
    }

    // ---DMN TESTS ---

    @ParameterizedTest
    @ValueSource(strings = ["green", "yellow", "red"])
    fun `simple dmn decision test`(color: String) {
        val procId = "simple-dmn-test"
        helper.saveAndDeployBpmnFromResource("test/dmn/$procId.bpmn.xml", procId)
        helper.saveAndDeployDmnFromResource("test/dmn/$procId.dmn.xml", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "color" to color
            )
        ).engine(processEngine).execute()

        val colorActionMapping = mapOf(
            "green" to "go",
            "yellow" to "prepare",
            "red" to "stop"
        )
        assertThat(scenario.instance(process)).variables().containsEntry("action", colorActionMapping[color])

        verify(process).hasFinished("endEvent")
    }

    @ParameterizedTest
    @ValueSource(strings = ["green", "yellow", "red"])
    fun `simple dmn decision with dmn decision ref test`(color: String) {
        val procId = "simple-dmn-test-with-dmn-decision-ref"
        helper.saveAndDeployBpmnFromResource("test/dmn/$procId.bpmn.xml", procId)
        helper.saveAndDeployDmnFromResource("test/dmn/simple-dmn-test.dmn.xml", "simple-dmn-test")

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "color" to color
            )
        ).engine(processEngine).execute()

        val colorActionMapping = mapOf(
            "green" to "go",
            "yellow" to "prepare",
            "red" to "stop"
        )
        assertThat(scenario.instance(process)).variables().containsEntry("action", colorActionMapping[color])

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `dmn with specific decision version`() {
        val procId = "dmn-test-specific-version"
        helper.saveAndDeployBpmnFromResource("test/dmn/$procId.bpmn.xml", procId)

        val dmnVersion1 = ResourceUtils.getFile("classpath:test/dmn/$procId.dmn.xml")
            .readText(StandardCharsets.UTF_8)
        helper.saveAndDeployDmnFromString(dmnVersion1, procId)

        val dmnVersion2 = dmnVersion1.replace("version 1", "version 2")
        helper.saveAndDeployDmnFromString(dmnVersion2, procId)

        val dmnVersion3 = dmnVersion1.replace("version 1", "version 3")
        helper.saveAndDeployDmnFromString(dmnVersion3, procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "any_input" to "any_value"
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("result_var", "version 2")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `dmn with specific decision version tag`() {
        val procId = "dmn-test-specific-version-tag"
        helper.saveAndDeployBpmnFromResource("test/dmn/$procId.bpmn.xml", procId)

        val dmnVersionTag1 = ResourceUtils.getFile("classpath:test/dmn/$procId.dmn.xml")
            .readText(StandardCharsets.UTF_8)
        helper.saveAndDeployDmnFromString(dmnVersionTag1, procId)

        val dmnVersionTag2 = dmnVersionTag1.replace("vt_version_tag_1", "vt_version_tag_2")
            .replace("version tag 1", "version tag 2")
        helper.saveAndDeployDmnFromString(dmnVersionTag2, procId)

        val dmnVersionTag3 = dmnVersionTag1.replace("vt_version_tag_1", "vt_version_tag_3")
            .replace("version tag 1", "version tag 3")
        helper.saveAndDeployDmnFromString(dmnVersionTag3, procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "any_input" to "any_value"
            )
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("result_var", "version tag 2")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `dmn test with required decisions and input with feel`() {
        val procId = "dmn-test-multiple-input-expression"
        helper.saveAndDeployBpmnFromResource("test/dmn/$procId.bpmn.xml", procId)
        helper.saveAndDeployDmnFromResource("test/dmn/$procId.dmn.xml", procId)

        val scenario = run(process).startByKey(
            procId,
            mapOf(
                "season" to "Spring",
                "guestCount" to 10,
                "guestsWithChildren" to true
            )
        ).engine(processEngine).execute()

        scenario.instance(process)

        assertThat(scenario.instance(process)).variables().containsEntry(
            "beveragesResult",
            listOf("Guiness", "Apple Juice")
        )

        verify(process).hasFinished("endEvent")
    }

    // ---SERVICE TASK TESTS ---

    @Test
    fun `test service task with expression and result variable`() {
        val procId = "test-service-task-expression"

        helper.saveAndDeployBpmn(SERVICE_TASK, procId)

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("foo", "bar")

        verify(process).hasFinished("endEvent")
    }

    // ---ERROR EVENTS TESTS ---

    @Test
    fun `test catch error event`() {
        val procId = "test-catch-error-from-service-task"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_event_error")
        verify(process, never()).hasFinished("end_event_success")
    }

    @Test
    fun `test catch error event with different catch code should not be catched`() {
        val procId = "test-catch-error-with-different-catch-code-from-service-task"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("end_event_success")
        verify(process, never()).hasFinished("end_event_error")
    }

    @Test
    fun `test catch error event with different catch code should be catched from error event without code`() {
        val procId = "test-catch-error-with-different-catch-code-and-catch-all-event"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_event_error_catch_all")
        verify(process, never()).hasFinished("end_event_error")
        verify(process, never()).hasFinished("end_event_success")
    }

    @Test
    fun `test catch error event with multiple catch code - catch code 1`() {
        val procId = "test-catch-error-with-multiple-catch"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "code" to "code_1"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_event_code_1")
        verify(process, never()).hasFinished("end_event_code_2")
        verify(process, never()).hasFinished("end_event_success")
    }

    @Test
    fun `test catch error event with multiple catch code - catch code 2`() {
        val procId = "test-catch-error-with-multiple-catch"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "code" to "code_2"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("end_event_code_2")
        verify(process, never()).hasFinished("end_event_code_1")
        verify(process, never()).hasFinished("end_event_success")
    }

    @Test
    fun `test error start event sub process`() {
        val procId = "test-error-start-event-sub-process"

        helper.saveAndDeployBpmn(ERROR, procId)

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("foo", "bar")

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test error boundary event sub process`() {
        val procId = "test-error-boundary-event-sub-process"

        helper.saveAndDeployBpmn(ERROR, procId)

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEventCatch")
        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `test catch error with code and message variables`() {
        val procId = "test-catch-error-with-variables"

        helper.saveAndDeployBpmn(ERROR, procId)

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("codeVariable", "its code")
        assertThat(scenario.instance(process)).variables().containsEntry("msgVariable", "its message")

        verify(process).hasFinished("end_event_error")
        verify(process, never()).hasFinished("end_event_success")
    }

    @Test
    fun `test error start event sub process - check message from variable`() {
        val procId = "test-error-start-event-sub-process-message-from-prop"

        helper.saveAndDeployBpmn(ERROR, procId)

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsEntry("foo", "bar")
        assertThat(scenario.instance(process)).variables().containsEntry("codeVariable", "error-code")
        assertThat(scenario.instance(process)).variables().containsEntry("messageVariable", "its throw message")

        verify(process).hasFinished("endEvent")
    }

    // ---TERMINATE EVENT TESTS ---

    @Test
    fun `terminate event should terminate process`() {
        val procId = "test-terminate-event"
        helper.saveAndDeployBpmn(ERROR, procId)

        val doc = "doc@terminate"

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to doc
            )
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")

        val taskCount = procTaskService.getTasksByDocument(doc).size
        assertThat(taskCount).isEqualTo(0)
    }

    // ---CONDITIONAL EVENT TESTS ---

    @Test
    fun `test condition expression catch event`() {
        val procId = "test-conditional-event-expression"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition expression catch event with predefined variable`() {
        val procId = "test-conditional-event-expression"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition script catch event`() {
        val procId = "test-conditional-event-script"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "not_bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition script catch event with predefined variable`() {
        val procId = "test-conditional-event-script"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with variable name`() {
        val procId = "test-conditional-event-variable-name"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with variable name not match`() {
        val procId = "test-conditional-event-variable-name-not-match"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with event - create`() {
        val procId = "test-conditional-event-variable-name-event-create"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with event - update`() {
        val procId = "test-conditional-event-variable-name-event-update"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "not_bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with event - delete`() {
        val procId = "test-conditional-event-variable-name-event-delete"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.removeVariable(it.processInstanceId, "foo")
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with multiple events`() {
        val procId = "test-conditional-event-variable-name-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "not_bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test condition catch event with events not match`() {
        val procId = "test-conditional-event-variable-name-event-not-match"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {
            runtimeService.setVariable(it.processInstanceId, "foo", "bar")
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "not_bar"
            )
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `test conditional intermediate catch event react on document change`() {
        val procId = "test-conditional-document-intermediate-catch-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        emptyList()
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `conditional event should not react on document change if reaction flag is false`() {
        val procId = "test-conditional-disabled-document-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        emptyList()
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `test conditional event with document variables react on document change`() {
        val procId = "test-conditional-document-intermediate-catch-with-document-variables-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        listOf(ChangedValue("name"))
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        Awaitility.await().atMost(15, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
        }
    }

    @Test
    fun `test conditional event with multiple document variables react on document change`() {
        val procId = "test-conditional-document-intermediate-catch-with-document-variables-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        listOf(
                            ChangedValue("name"),
                            ChangedValue("another_variable")
                        )
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test conditional event with document variables should not react on another variable change`() {
        val procId = "test-conditional-document-intermediate-catch-with-document-variables-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtConditionalIntermediateEvent("conditionalEvent")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        listOf(ChangedValue("another_variable"))
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
    }

    @Test
    fun `test conditional boundary event react on document change`() {
        val procId = "test-conditional-document-boundary-event"

        helper.saveAndDeployBpmn(CONDITIONAL, procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {

            documentRecordsDao.setRecord(
                docRef.getLocalId(),
                modifiedDocRecord
            )

            bpmnEventHelper.sendRecordChangedEvent(
                RecordChangedEventDto(
                    docRef,
                    Diff(
                        emptyList()
                    )
                )
            )
        }

        run(process).startByKey(
            procId,
            docRef.toString(),
            variables_docRef
        ).engine(processEngine).execute()

        verify(process, never()).hasFinished("endEvent")
        verify(process).hasFinished("endFromEvent")
    }

    // ---CALL ACTIVITY TESTS ---

    @Test
    fun `test call activity with participant`() {
        val procId = "test-call-activity-with-participant"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endMain")
    }

    @Test
    fun `test call activity with incorrect called element should throw`() {
        val procId = "test-call-activity-with-incorrect-called-element"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        assertThrows<ProcessEngineException> {
            run(process).startByKey(
                procId
            ).engine(processEngine).execute()
        }
    }

    @Test
    fun `test call activity with separate process`() {
        val procId = "test-call-activity-separate-process"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)
        helper.saveAndDeployBpmn(CALL_ACTIVITY, "test-call-activity-separate-process-called")

        run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity default variables propagation`() {
        val procId = "test-call-activity-default-variables-propagation"

        val processInitVariables = mapOf(
            BPMN_DOCUMENT_REF to docRef.toString(),
            BPMN_WORKFLOW_INITIATOR to "jhon",
            BPMN_DOCUMENT_TYPE to "docType"
        )

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            val allVars = runtimeService.getVariables(it.processInstanceId)

            assertThat(allVars).containsKeys(*DEFAULT_IN_VARIABLES_PROPAGATION_TO_CALL_ACTIVITY.toTypedArray())
            assertThat(allVars).containsAllEntriesOf(processInitVariables)

            it.complete()
        }

        run(process).startByKey(
            procId,
            processInitVariables
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity variables should not propagation without specifying`() {
        val procId = "test-call-activity-default-variables-propagation"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            val allVars = runtimeService.getVariables(it.processInstanceId)

            assertThat(allVars).doesNotContainKey("foo")

            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "foo" to "bar"
            )
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity input all variables propagation`() {
        val procId = "test-call-activity-input-all-variables-propagation"

        val processInitVariables = mapOf(
            "inputVar1" to "inputVal1",
            "inputVar2" to 2
        )

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            val allVars = runtimeService.getVariables(it.processInstanceId)

            assertThat(allVars).containsAllEntriesOf(processInitVariables)

            it.complete()
        }

        run(process).startByKey(
            procId,
            processInitVariables
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity input specified variables propagation`() {
        val procId = "test-call-activity-input-specified-variables-propagation"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            val allVars = runtimeService.getVariables(it.processInstanceId)

            assertThat(allVars).containsAllEntriesOf(
                mapOf(
                    "incomeVar1" to "sourceVal1",
                    "incomeVar2" to 2
                )
            )
            assertThat(allVars).doesNotContainKey("thisVariableShouldNotPropagation")

            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                "sourceVar1" to "sourceVal1",
                "sourceVar2" to 2,
                "thisVariableShouldNotPropagation" to "notPropagation"
            )

        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity output all variables propagation`() {
        val procId = "test-call-activity-output-all-variables-propagation"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsAllEntriesOf(
            mapOf(
                "innerVariable1" to "innerValue1",
                "innerVariable2" to "innerValue2"
            )
        )

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `test call activity output specified variables propagation`() {
        val procId = "test-call-activity-output-specified-variables-propagation"

        helper.saveAndDeployBpmn(CALL_ACTIVITY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        val scenario = run(process).startByKey(
            procId
        ).engine(processEngine).execute()

        assertThat(scenario.instance(process)).variables().containsAllEntriesOf(
            mapOf(
                "incomeVariable1" to "innerValue1",
                "incomeVariable2" to "innerValue2"
            )
        )
        assertThat(scenario.instance(process)).variables().doesNotContainKey("thisVariableShouldNotPropagation")

        verify(process).hasFinished("endEvent")
    }

    // ---BUSINESS KEY TESTS ---

    @Test
    fun `resolve business key by document test`() {
        val procId = "test-resolve-business-key"

        helper.saveAndDeployBpmn(BUSINESS_KEY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it.processInstance.businessKey).isEqualTo(docRef.toString())
            it.complete()
        }

        run(process).startByKey(
            procId,
            variables_docRef
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    @Test
    fun `resolve business key by source business key test`() {
        val procId = "test-resolve-business-key"

        helper.saveAndDeployBpmn(BUSINESS_KEY, procId)

        `when`(process.runsCallActivity("CallActivity")).thenReturn(Scenario.use(childProcess))

        `when`(childProcess.waitsAtUserTask("userTask")).thenReturn {
            assertThat(it.processInstance.businessKey).isEqualTo(docRef.toString())
            it.complete()
        }

        run(process).startByKey(
            procId,
            docRef.toString()
        ).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")
    }

    // ---KPI TESTS ---

    @Test
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    fun `kpi on user task duration from start to end user task`() {
        val procId = "test-kpi-duration"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "Activity_user_task",
                sourceEventType = BpmnKpiEventType.START,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.END
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isGreaterThan(1_000)
                }
            )
        }
    }

    @Test
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    fun `kpi on user task duration from start event to start user task`() {
        val procId = "test-kpi-duration"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "startEvent",
                sourceEventType = BpmnKpiEventType.START,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.START
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isLessThan(3_000)
                }
            )
        }
    }

    @Test
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    fun `kpi on user task duration from start event to user task end`() {
        val procId = "test-kpi-duration"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "startEvent",
                sourceEventType = BpmnKpiEventType.START,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.END
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isGreaterThan(1_000)
                }
            )
        }
    }

    @Test
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    fun `kpi on user task duration from start end event to user task end`() {
        val procId = "test-kpi-duration"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "startEvent",
                sourceEventType = BpmnKpiEventType.END,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.END
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isGreaterThan(1_000)
                }
            )
        }
    }

    @Test
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    fun `kpi on user task duration from start to end process`() {
        val procId = "test-kpi-duration"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "startEvent",
                sourceEventType = BpmnKpiEventType.START,
                target = "endEvent",
                targetEventType = BpmnKpiEventType.END
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isGreaterThan(1_000)
                }
            )
        }
    }

    @ParameterizedTest
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    @ValueSource(
        strings = [
            "startEvent", "subProcess", "startEventSubProcess", "userTask", "endEventSubProcess",
            "endEvent"
        ]
    )
    fun `kpi count start event`(activityId: String) {
        val procId = "test-kpi-count"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.COUNT,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                target = activityId,
                targetEventType = BpmnKpiEventType.START
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")

        `when`(bpmnKpiService.queryKpiValues(any(), any())).thenReturn(emptyList())

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(bpmnKpiService, Mockito.atLeast(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isEqualTo(1)
                }
            )
        }
    }

    @ParameterizedTest
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    @ValueSource(
        strings = [
            "startEvent", "subProcess", "startEventSubProcess", "userTask", "endEventSubProcess",
            "endEvent"
        ]
    )
    fun `kpi count end event`(activityId: String) {
        val procId = "test-kpi-count"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.COUNT,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                target = activityId,
                targetEventType = BpmnKpiEventType.END
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)

        `when`(process.waitsAtUserTask("userTask")).thenReturn {
            it.complete()
        }

        run(process).startByKey(procId, variables_docRef).engine(processEngine).execute()

        verify(process).hasFinished("endEvent")

        `when`(bpmnKpiService.queryKpiValues(any(), any())).thenReturn(emptyList())

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(bpmnKpiService, Mockito.atLeast(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isEqualTo(1)
                }
            )
        }
    }

    @ParameterizedTest
    @EnabledIf(
        expression = "#{environment['ecos-process.bpmn.elements.listener.enabled'] == 'true'}",
        loadContext = true
    )
    @ValueSource(strings = ["store/doc@1", "store/doc@2"])
    fun `kpi with dmn condition true test`(docRef: String) {
        val procId = "test-kpi-with-dmn"
        val dmnId = "dmn-kpi-test"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "Activity_user_task",
                sourceEventType = BpmnKpiEventType.START,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.END,
                dmnCondition = EntityRef.create(
                    AppName.EPROC,
                    DmnDecisionLatestRecords.ID,
                    "Decision_dmn-kpi-test"
                )
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)
        helper.saveAndDeployDmnFromResource("test/dmn/$dmnId.dmn.xml", dmnId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef
            )
        ).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(1)).createKpiValue(
                org.mockito.kotlin.check { kpiValue ->
                    assertThat(kpiValue.settingsRef).isEqualTo(
                        EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, settingsId)
                    )
                    assertThat(kpiValue.value.toLong()).isGreaterThan(1_000)
                }
            )
        }
    }

    @Test
    fun `kpi with dmn condition false test`() {
        val procId = "test-kpi-with-dmn"
        val dmnId = "dmn-kpi-test"
        val settingsId = UUID.randomUUID().toString()

        kpiSettings.add(
            helper.createDurationKpiSettings(
                id = settingsId,
                kpiType = BpmnKpiType.DURATION,
                process = EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, procId),
                source = "Activity_user_task",
                sourceEventType = BpmnKpiEventType.START,
                target = "Activity_user_task",
                targetEventType = BpmnKpiEventType.END,
                dmnCondition = EntityRef.create(
                    AppName.EPROC,
                    DmnDecisionLatestRecords.ID,
                    "Decision_dmn-kpi-test"
                )
            )
        )

        helper.saveAndDeployBpmnFromResource("test/bpmn/kpi/$procId.bpmn.xml", procId)
        helper.saveAndDeployDmnFromResource("test/dmn/$dmnId.dmn.xml", dmnId)

        `when`(process.waitsAtUserTask("Activity_user_task")).thenReturn {
            TimeUnit.SECONDS.sleep(3)
            it.complete()
        }

        run(process).startByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef3.toString()
            )
        ).engine(processEngine).execute()

        Awaitility.await().atMost(KPI_ASYNC_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted {
            verify(process).hasFinished("endEvent")
            verify(bpmnKpiService, Mockito.times(0)).createKpiValue(
                any()
            )
        }
    }

    // --- LAZY APPROVAL TESTS ---

    @Test
    fun `send lazy approval notification`() {
        val procId = "test-lazy-approval-simple-process"
        val defaultCommentKey = "lazy-approval-default-comment"
        val mailForAnswerKey = "lazy-approval-mail-for-reply"
        val taskTokenName = "tokenLA"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        val additionalMeta = mutableMapOf<String, Any>()

        `when`(ecosConfigService.getValue(defaultCommentKey)).thenReturn(DataValue.create("comment"))
        `when`(ecosConfigService.getValue(mailForAnswerKey)).thenReturn(DataValue.create("testLa@mail.test"))

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            additionalMeta["task_id"] = it.id
            additionalMeta["task_token"] = procTaskService.getVariableLocal(it.id, taskTokenName).toString()
            additionalMeta["default_comment"] = ecosConfigService.getValue(defaultCommentKey).asText()
            additionalMeta["mail_for_answer"] = ecosConfigService.getValue(mailForAnswerKey).asText()
            additionalMeta["process"] = mapOf(
                "documentRef" to "store/doc@1",
                "currentRunAsUser" to EntityRef.EMPTY,
                taskTokenName to procTaskService.getVariableLocal(it.id, taskTokenName)
            )

            it.complete()
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .recipients(listOf(germanRecord.email))
            .templateRef(EntityRef.valueOf("notifications/template@test-la-not"))
            .additionalMeta(additionalMeta)
            .build()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(
                    NotificationEqualsWrapper(
                        notification
                    )
                )
            }
        )

        verify(process).hasFinished("endEventApproved")
    }

    @Test
    fun `send lazy approval notification with custom template`() {
        val procId = "test-lazy-approval-with-custom-template-process"
        val defaultCommentKey = "lazy-approval-default-comment"
        val mailForAnswerKey = "lazy-approval-mail-for-reply"
        val taskTokenName = "tokenLA"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        val additionalMeta = mutableMapOf<String, Any>()

        `when`(ecosConfigService.getValue(defaultCommentKey)).thenReturn(DataValue.create("comment"))
        `when`(ecosConfigService.getValue(mailForAnswerKey)).thenReturn(DataValue.create("testLa@mail.test"))

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            additionalMeta["task_id"] = it.id
            additionalMeta["task_token"] = procTaskService.getVariableLocal(it.id, taskTokenName).toString()
            additionalMeta["default_comment"] = ecosConfigService.getValue(defaultCommentKey).asText()
            additionalMeta["mail_for_answer"] = ecosConfigService.getValue(mailForAnswerKey).asText()
            additionalMeta["process"] = mapOf(
                "documentRef" to "store/doc@1",
                "currentRunAsUser" to EntityRef.EMPTY,
                taskTokenName to procTaskService.getVariableLocal(it.id, taskTokenName),
                "laTemplateRefTest" to "notifications/template@test-la-notification",
            )

            it.complete()
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        val notification = Notification.Builder()
            .record(docRef)
            .notificationType(NotificationType.EMAIL_NOTIFICATION)
            .recipients(listOf(germanRecord.email))
            .templateRef(EntityRef.valueOf("notifications/template@test-la-notification"))
            .additionalMeta(additionalMeta)
            .build()

        verify(notificationService).send(
            org.mockito.kotlin.check {
                assertThat(NotificationEqualsWrapper(it)).isEqualTo(
                    NotificationEqualsWrapper(
                        notification
                    )
                )
            }
        )

        verify(process).hasFinished("endEventApproved")
    }

    @Test
    fun `approve lazy approval task`() {
        val procId = "test-lazy-approval-simple-process"
        val defaultCommentKey = "lazy-approval-default-comment"
        val mailForAnswerKey = "lazy-approval-mail-for-reply"
        val taskTokenName = "tokenLA"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        val additionalMeta = mutableMapOf<String, Any>()

        `when`(ecosConfigService.getValue(defaultCommentKey)).thenReturn(DataValue.create("comment"))
        `when`(ecosConfigService.getValue(mailForAnswerKey)).thenReturn(DataValue.create("testLa@mail.test"))

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            additionalMeta["task_id"] = it.id
            additionalMeta["task_token"] = procTaskService.getVariableLocal(it.id, taskTokenName).toString()

            bpmnLazyApprovalService.approveTask(
                taskId = it.id,
                taskOutcome = "Approve",
                userId = "german",
                token = procTaskService.getVariableLocal(it.id, taskTokenName).toString(),
                comment = "comment"
            )
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        verify(process).hasFinished("endEventApproved")
    }

    @Test
    fun `create task without lazy approval should not send la notification`() {
        val procId = "test-lazy-approval-without-la-simple-process"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            it.complete()
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        verify(bpmnLazyApprovalService, never()).sendNotification(any())

        verify(process).hasFinished("endEventApproved")
    }

    @Test
    fun `approve lazy approval task with wrong user should not allow complete task`() {
        val procId = "test-lazy-approval-simple-process"
        val defaultCommentKey = "lazy-approval-default-comment"
        val mailForAnswerKey = "lazy-approval-mail-for-reply"
        val taskTokenName = "tokenLA"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        val additionalMeta = mutableMapOf<String, Any>()

        `when`(ecosConfigService.getValue(defaultCommentKey)).thenReturn(DataValue.create("comment"))
        `when`(ecosConfigService.getValue(mailForAnswerKey)).thenReturn(DataValue.create("testLa@mail.test"))

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            additionalMeta["task_id"] = it.id
            additionalMeta["task_token"] = procTaskService.getVariableLocal(it.id, taskTokenName).toString()

            val approvalReport = bpmnLazyApprovalService.approveTask(
                taskId = it.id,
                taskOutcome = "Approve",
                userId = "petya",
                token = procTaskService.getVariableLocal(it.id, taskTokenName).toString(),
                comment = "comment"
            )

            assertThat(approvalReport.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        verify(process, never()).hasFinished("endEventApproved")
    }

    @Test
    fun `approve lazy approval task with wrong token should not allow complete task`() {
        val procId = "test-lazy-approval-simple-process"
        val defaultCommentKey = "lazy-approval-default-comment"
        val mailForAnswerKey = "lazy-approval-mail-for-reply"
        val taskTokenName = "tokenLA"

        EcosTestLicense.updateContent().addFeature("lazy-approval").update()

        helper.saveAndDeployBpmn(FOLDER_LA, procId)

        val additionalMeta = mutableMapOf<String, Any>()

        `when`(ecosConfigService.getValue(defaultCommentKey)).thenReturn(DataValue.create("comment"))
        `when`(ecosConfigService.getValue(mailForAnswerKey)).thenReturn(DataValue.create("testLa@mail.test"))

        `when`(process.waitsAtUserTask("testLazyApproveTask")).thenReturn {
            additionalMeta["task_id"] = it.id
            additionalMeta["task_token"] = procTaskService.getVariableLocal(it.id, taskTokenName).toString()

            val approvalReport = bpmnLazyApprovalService.approveTask(
                taskId = it.id,
                taskOutcome = "Approve",
                userId = "german",
                token = "bed-token",
                comment = "comment"
            )

            assertThat(approvalReport.processingCode).isEqualTo(MailProcessingCode.TOKEN_NOT_FOUND)
        }

        AuthContext.runAs(EmptyAuth) {
            run(process).startByKey(
                procId,
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString()
                )
            ).engine(processEngine).execute()
        }

        verify(process, never()).hasFinished("endEventApproved")
    }

    fun getSubscriptionsAfterAction(
        incomingEventData: IncomingEventData,
        action: () -> Unit
    ): List<EventSubscription> {
        val existingSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            incomingEventData
        ).map { it.event }

        action()

        val updatedSubscriptions = camundaEventSubscriptionFinder.getActualCamundaSubscriptions(
            incomingEventData
        ).map { it.event }

        return ListUtils.subtract(updatedSubscriptions, existingSubscriptions)
    }

    class PotterRecord(

        @AttName("email")
        val email: String = "harry.potter@hogwarts.com",

        @AttName("name")
        val name: String = "Harry Potter",

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@potter")

    )

    class UserIvanRecord(
        @AttName("email")
        val email: String = "ivan@mail.ru"
    )

    class UserPetyaRecord(
        @AttName("email")
        val email: String = "petya@mail.ru"
    )

    class UsersGroupRecord(
        @AttName("containedUsers")
        val containedUsers: List<EntityRef> = listOf(
            ivanRef,
            petyaRef
        )
    )

    class DocRecord(

        @AttName("name")
        val name: String = "Doc 1",

        @AttName("sum")
        val sum: Double = 5_500.0,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@document")
    )

    class ModifiedDocRecord(

        @AttName("name")
        val name: String = "Doc 1 modified",

        @AttName("sum")
        val sum: Double = 7_000.0,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@document")
    )

    class DocRecord2(

        @AttName("name")
        val name: String = "Doc 2",

        @AttName("sum")
        val sum: Double = 13_000.0,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@document")
    )

    class DocRecord3(

        @AttName("name")
        val name: String = "Doc 3",

        @AttName("sum")
        val sum: Double = 20_000.0,

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@document")
    )

    class UserGermanRecord(
        @AttName("email")
        val email: String = "german@mail.ru"
    )
}

/**
 * Wrap equals without id, recipients ignore order
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
        if (!CollectionUtils.isEqualCollection(dto.recipients, other.dto.recipients)) return false
        if (dto.from != other.dto.from) return false
        if (!CollectionUtils.isEqualCollection(dto.cc, other.dto.cc)) return false
        if (!CollectionUtils.isEqualCollection(dto.bcc, other.dto.bcc)) return false
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
