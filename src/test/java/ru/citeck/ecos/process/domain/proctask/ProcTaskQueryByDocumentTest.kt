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

/**
 * Query tasks by "document" attribute — the predicate used by task-form.
 *
 * A textual document value should match both documentRef and mainDocumentRef process variables.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcTaskQueryByDocumentTest {

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private const val TEST_USER = "testUser"
        private const val PROC_ID = "bpmn-task-query-document-atts-simple-task-create"
        private const val DOCUMENT_TYPE = "test-doc"

        private const val MAIN_DOCUMENT_REF_VAR = "mainDocumentRef"

        private val DOC_A = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@doc-a")
        private val DOC_B = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@doc-b")
        private val MAIN_DOC = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@main-doc")
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

        // documentRef only
        startProcess(DOC_A, mainDocumentRef = null)
        // documentRef differs from mainDocumentRef
        startProcess(DOC_B, mainDocumentRef = MAIN_DOC)
    }

    @AfterAll
    fun tearDown() {
        startedProcessIds.forEach { processId ->
            bpmnProcessService.deleteProcessInstance(processId)
        }
        helper.clearTasks()
    }

    @Test
    fun `query by document should find tasks by documentRef variable`() {
        val found = queryByDocument(DOC_A.toString())

        assertThat(found).hasSize(1)
        assertThat(getDocRefsFromTasks(found)).containsExactly(DOC_A.toString())
    }

    @Test
    fun `query by document should find tasks by mainDocumentRef variable`() {
        val found = queryByDocument(MAIN_DOC.toString())

        assertThat(found).hasSize(1)
        // the task belongs to the process started for DOC_B, MAIN_DOC is only its mainDocumentRef
        assertThat(getDocRefsFromTasks(found)).containsExactly(DOC_B.toString())
    }

    @Test
    fun `query by document should find tasks when documentRef and mainDocumentRef both exist`() {
        val found = queryByDocument(DOC_B.toString())

        assertThat(found).hasSize(1)
        assertThat(getDocRefsFromTasks(found)).containsExactly(DOC_B.toString())
    }

    @Test
    fun `query by unknown document should find nothing`() {
        assertThat(queryByDocument("eproc/$DOCUMENT_TYPE@unknown")).isEmpty()
    }

    /**
     * Documents current behaviour: mainDocumentRef fallback is applied to textual values only,
     * a list of documents is matched against documentRef alone.
     */
    @Test
    fun `query by document list should not fall back to mainDocumentRef`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.inVals(
                        ProcTaskSqlQueryBuilder.ATT_DOCUMENT,
                        listOf(DOC_A.toString(), MAIN_DOC.toString())
                    )
                )
            )
        }

        assertThat(found).hasSize(1)
        assertThat(getDocRefsFromTasks(found)).containsExactly(DOC_A.toString())
    }

    private fun queryByDocument(document: String): List<EntityRef> {
        return AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_DOCUMENT, document)
                )
            )
        }
    }

    private fun startProcess(docRef: EntityRef, mainDocumentRef: EntityRef?) {
        val variables = mutableMapOf<String, Any?>(
            BPMN_DOCUMENT_REF to docRef.toString(),
            BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE
        )
        mainDocumentRef?.let { variables[MAIN_DOCUMENT_REF_VAR] = it.toString() }

        val processInstance = bpmnProcessService.startProcess(
            StartProcessRequest(PROC_ID, docRef.toString(), variables)
        )

        startedProcessIds.add(processInstance.id)
    }

    private fun getDocRefsFromTasks(taskRefs: List<EntityRef>): List<String> {
        return recordsService.getAtts(taskRefs, mapOf("documentRef" to "documentRef?id"))
            .map { it.getAtt("documentRef").asText() }
    }
}
