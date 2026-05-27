package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.FormService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LA_COMPLETE_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BpmnEventEmitter
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.UserTaskEvent
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnUserTaskCompleteErrorEventTest {

    companion object {
        private const val PROC_ID = "bpmn-task-atts-document-simple-task-create"
        private const val USER_TASK_KEY = "UserTask"
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    @SpyBean
    private lateinit var camundaTaskFormService: FormService

    @SpyBean
    private lateinit var bpmnEventEmitter: BpmnEventEmitter

    @BeforeAll
    fun setUp() {
        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @AfterAll
    fun tearDown() {
        helper.cleanDeployments()
        helper.cleanDefinitions()
    }

    @AfterEach
    fun resetSpies() {
        reset(camundaTaskFormService, bpmnEventEmitter)
    }

    @Test
    fun `submitTaskForm error emits bpmn-user-task-complete-error event with proper payload`() {
        val docRef = EntityRef.valueOf("eproc/test-doc-error@${UUID.randomUUID()}")
        startProcessForDoc(docRef)

        val task = procTaskService.getTasksByDocument(docRef.toString())
            .firstOrNull() ?: error("user task not created for $docRef")

        val expectedError = RuntimeException("submit kaboom")
        doThrow(expectedError)
            .whenever(camundaTaskFormService)
            .submitTaskForm(eq(task.id), any<Map<String, Any?>>())

        val thrown = assertThrows<Exception> {
            AuthContext.runAsSystem {
                procTaskService.completeTask(
                    CompleteTaskData(
                        task = task,
                        outcome = Outcome(USER_TASK_KEY, "done", MLText("done")),
                        variables = mapOf(BPMN_LA_COMPLETE_KEY to true)
                    )
                )
            }
        }

        assertThat(thrown).hasMessageContaining("submit kaboom")

        val captor = argumentCaptor<UserTaskEvent>()
        verify(bpmnEventEmitter, times(1)).emitUserTaskCompleteErrorEvent(captor.capture())

        val event = captor.firstValue
        assertThat(event.errorMessage).isEqualTo("submit kaboom")
        assertThat(event.errorStackTrace).contains("submit kaboom")
        assertThat(event.elementDefId).isEqualTo(USER_TASK_KEY)
        assertThat(event.document).isEqualTo(docRef)
        assertThat(event.processId).isEqualTo(PROC_ID)
        assertThat(event.isCompletedViaMail).isTrue()

        val procDefRef = event.procDefRef ?: error("procDefRef must not be null")
        assertThat(procDefRef.getAppName()).isEqualTo(AppName.EPROC)
        assertThat(procDefRef.getSourceId()).isEqualTo(BpmnProcessDefRecords.ID)
        // procDefRef.localId must be ECOS-records id (rev.procDefId), NOT camunda's "key:version:deploymentId"
        assertThat(procDefRef.getLocalId()).isNotBlank().doesNotContain(":")
        assertThat(event.procDefId).isEqualTo(procDefRef.getLocalId())
        assertThat(event.procDeploymentVersion).isNotNull().isGreaterThan(0)
    }

    @Test
    fun `successful completeTask emits complete event via DelegateTask path with proper procDefRef`() {
        val docRef = EntityRef.valueOf("eproc/test-doc-success@${UUID.randomUUID()}")
        startProcessForDoc(docRef)

        val task = procTaskService.getTasksByDocument(docRef.toString())
            .firstOrNull() ?: error("user task not created for $docRef")

        AuthContext.runAsSystem {
            procTaskService.completeTask(
                CompleteTaskData(
                    task = task,
                    outcome = Outcome(USER_TASK_KEY, "done", MLText("done")),
                    variables = emptyMap()
                )
            )
        }

        verify(bpmnEventEmitter, times(0)).emitUserTaskCompleteErrorEvent(any())

        val captor = argumentCaptor<UserTaskEvent>()
        verify(bpmnEventEmitter, times(1)).emitUserTaskCompleteEvent(captor.capture())

        val event = captor.firstValue
        assertThat(event.errorMessage).isNull()
        assertThat(event.elementDefId).isEqualTo(USER_TASK_KEY)
        assertThat(event.processId).isEqualTo(PROC_ID)
        assertThat(event.document).isEqualTo(docRef)

        val procDefRef = event.procDefRef ?: error("procDefRef must not be null")
        assertThat(procDefRef.getSourceId()).isEqualTo(BpmnProcessDefRecords.ID)
        assertThat(procDefRef.getLocalId()).isNotBlank().doesNotContain(":")
        assertThat(event.procDefId).isEqualTo(procDefRef.getLocalId())
    }

    private fun startProcessForDoc(docRef: EntityRef) {
        AuthContext.runAsSystem {
            bpmnProcessService.startProcess(
                StartProcessRequest(
                    processId = PROC_ID,
                    businessKey = null,
                    variables = mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        BPMN_DOCUMENT_TYPE to "test"
                    )
                )
            )
        }
    }
}
