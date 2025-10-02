package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.proctask.service.ATT_CURRENT_USER_WITH_AUTH
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcTaskQueryWithDocumentAttsTest {

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var camundaTaskService: org.camunda.bpm.engine.TaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private const val TEST_USER = "testUser"
        private const val PROC_ID = "bpmn-task-query-document-atts-simple-task-create"
        private const val DOCUMENT_TYPE = "test-doc"

        private const val DOCUMENT_STATUS_VAR = "_doc_documentStatus"
        private const val DOCUMENT_TYPE_VAR = "_doc_documentType"
        private const val CUSTOM_VAR = "_doc_customField"
    }

    private val startedProcessIds = mutableListOf<String>()

    @BeforeAll
    fun setUp() {
        helper.clearTasks()

        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )

        createProcessWithVariables(
            "doc1",
            mapOf(
                DOCUMENT_STATUS_VAR to "NEW",
                DOCUMENT_TYPE_VAR to "contract"
            )
        )

        createProcessWithVariables(
            "doc2",
            mapOf(
                DOCUMENT_STATUS_VAR to "COMPLETED",
                DOCUMENT_TYPE_VAR to "invoice",
                CUSTOM_VAR to "customValue"
            )
        )

        createProcessWithVariables(
            "doc3",
            mapOf(
                DOCUMENT_STATUS_VAR to "FAILED",
                DOCUMENT_TYPE_VAR to "report"
            )
        )

        createProcessWithVariables(
            "doc4",
            mapOf(
                DOCUMENT_STATUS_VAR to "COMPLETED",
                DOCUMENT_TYPE_VAR to "contract",
                CUSTOM_VAR to "anotherValue"
            )
        )

        createProcessWithVariables(
            "doc5",
            mapOf(
                DOCUMENT_STATUS_VAR to "NEW",
                DOCUMENT_TYPE_VAR to "invoice"
            )
        )
    }

    @AfterAll
    fun tearDown() {
        startedProcessIds.forEach { processId ->
            bpmnProcessService.deleteProcessInstance(processId)
        }
        helper.clearTasks()
    }

    @Test
    fun `query multiple values with one attribute`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.or(
                        Predicates.eq(DOCUMENT_STATUS_VAR, "COMPLETED"),
                        Predicates.eq(DOCUMENT_STATUS_VAR, "FAILED")
                    )
                )
            )
        }

        assertThat(found).hasSize(3)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc2", "doc3", "doc4")
    }

    @Test
    fun `query different attributes`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(DOCUMENT_STATUS_VAR, "COMPLETED"),
                    Predicates.eq(DOCUMENT_TYPE_VAR, "contract")
                )
            )
        }

        assertThat(found).hasSize(1)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc4")
    }

    @Test
    fun `query with in predicate`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.`in`(DOCUMENT_TYPE_VAR, listOf("contract", "invoice"))
                )
            )
        }

        assertThat(found).hasSize(4)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc1", "doc2", "doc4", "doc5")
    }

    @Test
    fun `should handle empty variable conditions`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.empty(CUSTOM_VAR)
                )
            )
        }

        assertThat(found).hasSize(3)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc1", "doc3", "doc5")
    }

    @Test
    fun `should handle not empty variable conditions`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.notEmpty(CUSTOM_VAR)
                )
            )
        }

        assertThat(found).hasSize(2)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc2", "doc4")
    }

    @Test
    fun `should handle like multiple like attribute`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.or(
                        Predicates.like(DOCUMENT_TYPE_VAR, "contract"),
                        Predicates.like(DOCUMENT_TYPE_VAR, "tract")
                    )
                )
            )
        }

        assertThat(found).hasSize(2)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc1", "doc4")
    }

    @Test
    fun `should handle complex mixed conditions`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.or(
                        Predicates.and(
                            Predicates.eq(DOCUMENT_STATUS_VAR, "NEW"),
                            Predicates.eq(DOCUMENT_TYPE_VAR, "contract")
                        ),
                        Predicates.and(
                            Predicates.eq(DOCUMENT_STATUS_VAR, "COMPLETED"),
                            Predicates.notEmpty(CUSTOM_VAR)
                        )
                    )
                )
            )
        }

        assertThat(found).hasSize(3)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc1", "doc2", "doc4")
    }

    private fun createProcessWithVariables(docId: String, variables: Map<String, String>) {
        val docRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@$docId")

        val processInstance = bpmnProcessService.startProcess(
            StartProcessRequest(
                "",
                PROC_ID,
                docRef.toString(),
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString(),
                    BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE
                )
            )
        )

        startedProcessIds.add(processInstance.id)

        val tasks = camundaTaskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .list()

        tasks.forEach { task ->
            variables.forEach { (key, value) ->
                camundaTaskService.setVariableLocal(task.id, key, value)
            }
        }
    }

    private fun getDocIdsFromTasks(taskRefs: List<EntityRef>): List<String> {
        return recordsService.getAtts(taskRefs, mapOf("documentRef" to "documentRef?id"))
            .map { it.getAtt("documentRef").asText() }
            .map { EntityRef.valueOf(it).getLocalId() }
    }
}
