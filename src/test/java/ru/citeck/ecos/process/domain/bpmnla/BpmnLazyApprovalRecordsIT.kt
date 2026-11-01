package ru.citeck.ecos.process.domain.bpmnla

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.impl.util.BitMaskUtil
import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.instance.ServiceTask
import org.camunda.bpm.model.bpmn.instance.UserTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.lazyapproval.dto.LazyApprovalReportDto
import ru.citeck.ecos.lazyapproval.model.MailProcessingCode
import ru.citeck.ecos.license.EcosTestLicense
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_COMMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LAST_TASK_COMPLETOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LA_COMPLETE_KEY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_COMPLETED_BY
import ru.citeck.ecos.process.domain.bpmn.io.NS_ECOS_BPMN
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

/**
 * Exercises the registered LA DAO, production approval/Records/task services and PostgreSQL.
 * Only external persons/documents and the test BPMN's downstream business action are fixtures.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class, BpmnLazyApprovalRecordsIT.Config::class])
@Timeout(120)
class BpmnLazyApprovalRecordsIT {

    @Autowired private lateinit var records: RecordsService
    @Autowired private lateinit var tasks: TaskService
    @Autowired private lateinit var runtime: RuntimeService
    @Autowired private lateinit var history: HistoryService
    @Autowired private lateinit var helper: BpmnProcHelper
    @Autowired private lateinit var commands: CommandExecutor
    @Autowired private lateinit var procTasks: ProcTaskService
    @Autowired private lateinit var converter: CacheableTaskConverter
    @Autowired private lateinit var probe: ApprovalProbe

    private lateinit var taskId: String
    private lateinit var instanceId: String
    private lateinit var executionId: String
    private lateinit var token: String
    private var originalStatementTimeout: Int? = null
    private val document = EntityRef.valueOf("eproc/la-pipeline-document@one")

    @BeforeEach
    fun setUp() {
        AuthContext.runAsSystem {
            EcosTestLicense.updateContent().addFeature("lazy-approval").update()
            records.register(
                RecordsDaoBuilder.create("emodel/person")
                    .addRecord(APPROVER, Person("approver@example.test"))
                    .addRecord(OTHER, Person("other@example.test")).build()
            )
            records.register(
                RecordsDaoBuilder.create("la-pipeline-document").addRecord("one", Document()).build()
            )
            val key = "la-records-${UUID.randomUUID()}"
            val model = Bpmn.createExecutableProcess(key).startEvent()
                .subProcess("scope").embeddedSubProcess().startEvent()
                .userTask("approve").name("Approval")
                .serviceTask("businessAction").camundaDelegateExpression("\${laPipelineProbe}")
                .userTask("afterApproval").endEvent().subProcessDone().endEvent().done()
            model.definitions.setAttributeValueNs(NS_ECOS_BPMN, "processDefId", key)
            model.definitions.setAttributeValueNs(NS_ECOS_BPMN, "defState", "CONVERTED")
            for (id in listOf("approve", "afterApproval")) {
                val task = model.getModelElementById<UserTask>(id)
                task.setAttributeValueNs(NS_ECOS_BPMN, "manualRecipientsMode", "true")
                task.setAttributeValueNs(NS_ECOS_BPMN, "manualRecipients", "[\"$APPROVER\"]")
                task.setAttributeValueNs(NS_ECOS_BPMN, "outcomes", "[{\"id\":\"approve\",\"name\":{\"en\":\"Approve\"}}]")
                task.setAttributeValueNs(NS_ECOS_BPMN, "name_ml", "{\"en\":\"Approval\"}")
                // Receiving an approval is under test; avoid sending actual email on task creation.
                task.setAttributeValueNs(NS_ECOS_BPMN, "laEnabled", "false")
                task.setAttributeValueNs(NS_ECOS_BPMN, "laReportEnabled", "true")
                task.setAttributeValueNs(NS_ECOS_BPMN, "laSuccessReportNotificationTemplate", SUCCESS_TEMPLATE)
                task.setAttributeValueNs(NS_ECOS_BPMN, "laErrorReportNotificationTemplate", ERROR_TEMPLATE)
            }
            val businessAction = model.getModelElementById<ServiceTask>("businessAction")
            businessAction.setAttributeValueNs(NS_ECOS_BPMN, "serviceTaskType", "EXPRESSION")
            businessAction.setAttributeValueNs(NS_ECOS_BPMN, "expression", "\${laPipelineProbe.execute(execution)}")
            helper.saveAndDeployBpmnFromString(Bpmn.convertToString(model), key)
            instanceId = runtime.startProcessInstanceByKey(key, mapOf(BPMN_DOCUMENT_REF to document.toString())).id
            val task = tasks.createTaskQuery().processInstanceId(instanceId).singleResult()
            taskId = task.id
            executionId = task.executionId
            token = UUID.randomUUID().toString()
            tasks.setVariableLocal(taskId, "tokenLA", token)
            assertThat(task.assignee).isEqualTo(APPROVER)
            converter.removeFromActualTaskCache(taskId)
            probe.taskId = taskId
            probe.fail = false
            probe.invocations = 0
            commands.execute { ctx ->
                val config = ctx.dbSqlSession.sqlSession.configuration
                originalStatementTimeout = config.defaultStatementTimeout
                config.defaultStatementTimeout = 3
            }
        }
    }

    @AfterEach
    fun cleanUp() {
        AuthContext.runAsSystem {
            commands.execute { ctx ->
                ctx.dbSqlSession.sqlSession.configuration.defaultStatementTimeout = originalStatementTimeout
            }
            helper.cleanDeployments()
            helper.cleanDefinitions()
        }
    }

    @Test
    fun `real LA Records approval completes as mail sender and persists outcome comment and mail flag`() {
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.OK)
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isZero()
        assertThat(tasks.createTaskQuery().processInstanceId(instanceId).singleResult().taskDefinitionKey)
            .isEqualTo("afterApproval")
        assertThat(probe.invocations).isEqualTo(1)
        assertThat(probe.user).isEqualTo(APPROVER)
        assertThat(probe.outcome).isEqualTo("approve")
        assertThat(probe.comment).isEqualTo("approved comment")
        assertThat(probe.completor).isEqualTo(APPROVER)
        assertThat(
            history.createHistoricVariableInstanceQuery().taskIdIn(taskId)
                .variableName(BPMN_LA_COMPLETE_KEY).singleResult().value
        ).isEqualTo(true)
        assertThat(
            history.createHistoricVariableInstanceQuery().taskIdIn(taskId)
                .variableName(BPMN_TASK_COMPLETED_BY).singleResult().value
        ).isEqualTo(APPROVER)
        assertTemplates(report)
    }

    @Test
    fun `real actor permission check rejects a different mail sender without changing task`() {
        val report = approve(email = "other@example.test")
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        assertThat(report.errorMessage).contains("not a task actor")
        assertUnchangedTask()
        assertTemplates(report)
    }

    @Test
    fun `real completion rollback restores task and variables after downstream BPMN failure`() {
        probe.fail = true
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
        assertThat(report.errorMessage).contains("downstream approval failure")
        assertThat(probe.invocations).isEqualTo(1)
        assertThat(probe.sawDeletedTask).isTrue()
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isEqualTo(1)
        assertThat(tasks.getVariableLocal(taskId, "tokenLA")).isEqualTo(token)
        assertThat(tasks.getVariableLocal(taskId, BPMN_LA_COMPLETE_KEY)).isNull()
        assertThat(runtime.getVariable(executionId, "approve_outcome")).isNull()
        assertThat(runtime.getVariable(executionId, "downstreamWrite")).isNull()
        assertTemplates(report)
        // The original task is still usable after rollback, not merely present in a query result.
        probe.fail = false
        assertThat(approve().processingCode).isEqualTo(MailProcessingCode.OK)
    }

    @Test
    fun `approval rollback does not discard an independent caller transaction write`() {
        AuthContext.runAsSystem {
            val callerTask = tasks.newTask()
            tasks.saveTask(callerTask)
            try {
                tasks.setVariableLocal(callerTask.id, "callerMarker", "before")
                probe.fail = true
                val report = approve(beforeApproval = {
                    tasks.setVariableLocal(callerTask.id, "callerMarker", "preserved")
                })
                assertThat(report.processingCode).isEqualTo(MailProcessingCode.EXCEPTION)
                assertThat(report.errorMessage).contains("downstream approval failure")
                assertThat(tasks.getVariableLocal(callerTask.id, "callerMarker")).isEqualTo("preserved")
                assertThat(tasks.createTaskQuery().taskId(taskId).count()).isEqualTo(1)
                assertThat(runtime.getVariable(executionId, "downstreamWrite")).isNull()
            } finally {
                tasks.deleteTask(callerTask.id, true)
            }
        }
    }

    @Test
    fun `invalid token and outcome are rejected by real LA path`() {
        assertThat(approve(tokenValue = "invalid").processingCode).isEqualTo(MailProcessingCode.TOKEN_NOT_FOUND)
        assertUnchangedTask()
        assertThat(approve(outcome = "invalid").processingCode).isEqualTo(MailProcessingCode.OUTCOME_NOT_FOUND)
        assertUnchangedTask()
    }

    @Test
    fun `repeat approval resolves real historic task without repeating business action`() {
        assertThat(approve().processingCode).isEqualTo(MailProcessingCode.OK)
        val repeat = approve()
        assertThat(repeat.processingCode).isEqualTo(MailProcessingCode.TASK_ALREADY_COMPLETED)
        assertThat(probe.invocations).isEqualTo(1)
        assertTemplates(repeat)
    }

    @Test
    fun `cold real task read can flush execution cache state and approval still completes`() {
        markExecutionVariablesUnloaded()
        val before = executionState()
        AuthContext.runAsSystem {
            TxnContext.doInNewTxn { procTasks.getTaskById(taskId) }
        }
        val after = executionState()
        assertThat(after.first).isGreaterThan(before.first)
        assertThat(BitMaskUtil.isBitOn(after.second, ExecutionEntity.VARIABLES_STATE_BIT)).isFalse()
        // Restore the legitimate unknown-cache state and evict DTO: approval must perform the same read.
        markExecutionVariablesUnloaded()
        converter.removeFromActualTaskCache(taskId)
        val report = approve()
        assertThat(report.processingCode).isEqualTo(MailProcessingCode.OK)
        assertThat(tasks.createTaskQuery().taskId(taskId).count()).isZero()
    }

    private fun markExecutionVariablesUnloaded() {
        AuthContext.runAsSystem {
            commands.execute { ctx ->
                val connection = ctx.dbSqlSession.sqlSession.connection
                connection.prepareStatement("SELECT count(*) FROM ACT_RU_VARIABLE WHERE EXECUTION_ID_ = ? AND TASK_ID_ IS NULL").use {
                    it.setString(1, executionId)
                    it.executeQuery().use { result ->
                        result.next()
                        assertThat(result.getInt(1)).isZero()
                    }
                }
                // A set VARIABLES_STATE_BIT means unknown or nonempty, not that a variable must exist.
                connection.prepareStatement("UPDATE ACT_RU_EXECUTION SET CACHED_ENT_STATE_ = CACHED_ENT_STATE_ | ? WHERE ID_ = ?").use {
                    it.setInt(1, BitMaskUtil.getMaskForBit(ExecutionEntity.VARIABLES_STATE_BIT))
                    it.setString(2, executionId)
                    assertThat(it.executeUpdate()).isEqualTo(1)
                }
            }
        }
    }

    private fun executionState(): Pair<Int, Int> {
        return commands.execute { ctx ->
            ctx.dbSqlSession.sqlSession.connection.prepareStatement("SELECT REV_, CACHED_ENT_STATE_ FROM ACT_RU_EXECUTION WHERE ID_ = ?").use {
                it.setString(1, executionId)
                it.executeQuery().use { result ->
                    result.next()
                    result.getInt(1) to result.getInt(2)
                }
            }
        }
    }

    private fun approve(
        email: String = "approver@example.test",
        tokenValue: String = token,
        outcome: String = "approve",
        beforeApproval: () -> Unit = {}
    ): LazyApprovalReportDto {
        return AuthContext.runAsSystem {
            TxnContext.doInNewTxn {
                beforeApproval()
                records.mutateAndGetAtts(
                    EntityRef.valueOf("eproc/lazy-approval@"),
                    mapOf(
                        "taskId" to taskId,
                        "token" to tokenValue,
                        "outcome" to outcome,
                        "comment" to "approved\u00a0comment",
                        "type" to "EMAIL",
                        "email" to email
                    ),
                    LazyApprovalReportDto::class.java
                )
            }
        }
    }

    private fun assertUnchangedTask() {
        assertThat(tasks.createTaskQuery().taskId(taskId).singleResult().assignee).isEqualTo(APPROVER)
        assertThat(tasks.getVariableLocal(taskId, "tokenLA")).isEqualTo(token)
        assertThat(tasks.getVariableLocal(taskId, BPMN_LA_COMPLETE_KEY)).isNull()
        assertThat(probe.invocations).isZero()
    }

    private fun assertTemplates(report: LazyApprovalReportDto) {
        assertThat(report.laReportEnabled).isTrue()
        assertThat(report.laSuccessReportNotificationTemplate).isEqualTo(EntityRef.valueOf(SUCCESS_TEMPLATE))
        assertThat(report.laErrorReportNotificationTemplate).isEqualTo(EntityRef.valueOf(ERROR_TEMPLATE))
        assertThat(report.documentRef).isEqualTo(document)
    }

    class Person(@AttName("email") val email: String)
    class Document(@AttName("_type") val type: EntityRef = EntityRef.valueOf("emodel/type@document"))

    class ApprovalProbe(private val tasks: TaskService) : JavaDelegate {
        var taskId = ""
        var fail = false
        var invocations = 0
        var sawDeletedTask = false
        var user = ""
        var outcome: Any? = null
        var comment: Any? = null
        var completor: Any? = null

        override fun execute(execution: DelegateExecution) {
            invocations++
            user = AuthContext.getCurrentUser()
            outcome = execution.getVariable("approve_outcome")
            comment = execution.getVariable(BPMN_COMMENT)
            completor = execution.getVariable(BPMN_LAST_TASK_COMPLETOR)
            sawDeletedTask = (tasks.createTaskQuery().taskId(taskId).singleResult() as TaskEntity).isDeleted
            execution.setVariable("downstreamWrite", "must roll back on failure")
            if (fail) error("downstream approval failure")
        }
    }

    @TestConfiguration
    class Config {
        @Bean
        fun laPipelineProbe(tasks: TaskService): ApprovalProbe = ApprovalProbe(tasks)
    }

    companion object {
        private const val APPROVER = "la-pipeline-approver"
        private const val OTHER = "la-pipeline-other"
        private const val SUCCESS_TEMPLATE = "notifications/template@la-pipeline-success"
        private const val ERROR_TEMPLATE = "notifications/template@la-pipeline-error"
    }
}
