package ru.citeck.ecos.process.domain.bpmnla

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.instance.UserTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.lazyapproval.dto.LazyApprovalReportDto
import ru.citeck.ecos.lazyapproval.model.MailProcessingCode
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.TaskDefinitionUtils
import ru.citeck.ecos.process.domain.bpmn.io.NS_ECOS_BPMN
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmnla.services.BpmnLazyApprovalService
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.UUID

/**
 * Real platform transactions and Camunda/PostgreSQL. The read fixture deliberately flushes a
 * task variable to model a command with pending changes; it does not claim that every DTO read writes.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@Timeout(120)
class BpmnLazyApprovalTransactionIT {

    @Autowired private lateinit var tasks: TaskService
    @Autowired private lateinit var runtime: RuntimeService
    @Autowired private lateinit var repository: RepositoryService
    @Autowired private lateinit var commands: CommandExecutor

    private lateinit var service: BpmnLazyApprovalService
    private lateinit var procTasks: ProcTaskService
    private lateinit var historicTasks: ProcHistoricTaskService
    private lateinit var dto: ProcTaskDto
    private lateinit var deploymentId: String
    private lateinit var taskId: String
    private var failReportRead = false
    private var failCompletion = false
    private var mutationCount = 0
    private val readTransactions = mutableListOf<Any?>()
    private val reportTransactions = mutableListOf<Any?>()

    @BeforeEach
    fun setUp() {
        AuthContext.runAsSystem {
            val key = "la-txn-${UUID.randomUUID()}"
            val model = Bpmn.createExecutableProcess(key).startEvent().userTask("approve").endEvent().done()
            val task = model.getModelElementById<UserTask>("approve")
            task.setAttributeValueNs(NS_ECOS_BPMN, "laReportEnabled", "true")
            task.setAttributeValueNs(NS_ECOS_BPMN, "laSuccessReportNotificationTemplate", SUCCESS_TEMPLATE)
            task.setAttributeValueNs(NS_ECOS_BPMN, "laErrorReportNotificationTemplate", ERROR_TEMPLATE)
            deploymentId = repository.createDeployment().addModelInstance("$key.bpmn", model).deploy().id
            val instance = runtime.startProcessInstanceByKey(key)
            taskId = tasks.createTaskQuery().processInstanceId(instance.id).singleResult().id
            tasks.setVariableLocal(taskId, "tokenLA", "valid-token")
            tasks.setVariableLocal(taskId, "readMarker", "before")
            dto = ProcTaskDto(
                id = taskId,
                created = Instant.now(),
                name = MLText("Approval"),
                definitionKey = "approve",
                processDefinitionId = instance.processDefinitionId,
                documentRef = EntityRef.valueOf("eproc/test@document"),
                possibleOutcomes = listOf(TaskOutcome("approve", MLText("Approve")))
            )
        }
        procTasks = mock()
        historicTasks = mock()
        whenever(procTasks.getTaskById(taskId)).thenAnswer {
            readTransactions += TxnContext.getTxn().getId()
            tasks.setVariableLocal(taskId, "readMarker", "read")
            dto
        }
        whenever(procTasks.getVariableLocal(taskId, "tokenLA")).thenAnswer {
            readTransactions += TxnContext.getTxn().getId()
            tasks.getVariableLocal(taskId, "tokenLA")
        }
        val records = mock<RecordsService>()
        whenever(records.mutate(any<RecordAtts>())).thenAnswer {
            mutationCount++
            // Bound the red regression: old code blocks on its outer transaction's variable lock.
            commands.execute { context ->
                context.dbSqlSession.sqlSession.connection.createStatement().use {
                    it.execute("SET LOCAL lock_timeout = '1500ms'")
                }
            }
            tasks.complete(taskId)
            if (failCompletion) {
                error("completion failed after delete")
            }
            EntityRef.valueOf("eproc/proc-task@$taskId")
        }
        val repoReads = mock<RepositoryService>()
        whenever(repoReads.getProcessModel(any())).thenAnswer {
            reportTransactions += TxnContext.getTxn().getId()
            if (failReportRead) {
                error("report model unavailable")
            }
            repository.getProcessModel(it.getArgument(0))
        }
        // Fresh instance means the task-definition cache is cold for each test.
        val definitions = TaskDefinitionUtils(repoReads, mock())
        service = BpmnLazyApprovalService(
            procTasks, historicTasks, mock(), mock(), mock(), mock(), records, mock(), definitions
        )
    }

    @AfterEach
    fun cleanUp() {
        AuthContext.runAsSystem {
            repository.deleteDeployment(deploymentId, true)
        }
    }

    @Test
    fun `completion does not wait on a lock acquired while reading the task`() {
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.OK)
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isZero()
        assertTemplates(report)
    }

    @Test
    fun `completion failure rolls back reads and deletion but returns report without outer database reads`() {
        failCompletion = true
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        assertThat(report.errorMessage).contains("completion failed after delete")
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isEqualTo(1)
        assertThat(tasks.getVariableLocal(taskId, "readMarker")).isEqualTo("before")
        assertTemplates(report)
    }

    @Test
    fun `invalid token leaves task open and builds report inside isolated transaction`() {
        val report = approve(token = "invalid")
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.TOKEN_NOT_FOUND)
        assertThat(mutationCount).isZero()
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isEqualTo(1)
        assertTemplates(report)
    }

    @Test
    fun `invalid outcome leaves task open and builds report inside isolated transaction`() {
        val report = approve(outcome = "missing")
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.OUTCOME_NOT_FOUND)
        assertThat(mutationCount).isZero()
        assertTemplates(report)
    }

    @Test
    fun `completed task report reads cold definition cache inside isolated transaction`() {
        doAnswer { null }.whenever(procTasks).getTaskById(taskId)
        whenever(historicTasks.getHistoricTaskById(taskId)).thenAnswer {
            readTransactions += TxnContext.getTxn().getId()
            dto.copy(ended = Instant.now())
        }
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.TASK_ALREADY_COMPLETED)
        assertThat(mutationCount).isZero()
        assertTemplates(report)
    }

    @Test
    fun `missing task returns not found without completion`() {
        doAnswer { null }.whenever(procTasks).getTaskById(taskId)
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.TASK_NOT_FOUND)
        assertThat(mutationCount).isZero()
    }

    @Test
    fun `inactive historic task returns exception report without completing`() {
        doAnswer { null }.whenever(procTasks).getTaskById(taskId)
        whenever(historicTasks.getHistoricTaskById(taskId)).thenAnswer {
            readTransactions += TxnContext.getTxn().getId()
            dto
        }
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        assertThat(mutationCount).isZero()
        assertTemplates(report)
    }

    @Test
    fun `report lookup failure preserves original error without another database lookup`() {
        failReportRead = true
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        assertThat(report.errorMessage).contains("report model unavailable")
        assertThat(report.documentRef).isEqualTo(dto.documentRef)
        assertThat(reportTransactions).hasSize(1)
        assertThat(mutationCount).isZero()
        assertThat(tasks.getVariableLocal(taskId, "readMarker")).isEqualTo("before")
    }

    private fun approve(token: String = "valid-token", outcome: String = "approve"): LazyApprovalReportDto {
        return AuthContext.runAsSystem {
            TxnContext.doInNewTxn {
                val outer = TxnContext.getTxn().getId()
                val report = service.approveTask(taskId, outcome, "system", token, "comment")
                assertThat(TxnContext.getTxn().getId()).isEqualTo(outer)
                assertThat(readTransactions).doesNotContain(outer)
                assertThat(reportTransactions).doesNotContain(outer)
                report
            }
        }
    }

    private fun assertTemplates(report: LazyApprovalReportDto) {
        assertThat(report.laReportEnabled).isTrue()
        assertThat(report.laSuccessReportNotificationTemplate).isEqualTo(EntityRef.valueOf(SUCCESS_TEMPLATE))
        assertThat(report.laErrorReportNotificationTemplate).isEqualTo(EntityRef.valueOf(ERROR_TEMPLATE))
        assertThat(report.documentRef).isEqualTo(dto.documentRef)
        assertThat(report.taskName).isEqualTo(dto.name)
        assertThat(reportTransactions).isNotEmpty()
    }

    companion object {
        private const val SUCCESS_TEMPLATE = "notifications/template@la-success"
        private const val ERROR_TEMPLATE = "notifications/template@la-error"
    }
}
