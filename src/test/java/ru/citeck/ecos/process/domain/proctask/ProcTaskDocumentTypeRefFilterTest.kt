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
class ProcTaskDocumentTypeRefFilterTest {

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

        // Create processes with different document types
        createProcess("doc-contract-1", "ecos-contract")
        createProcess("doc-contract-2", "ecos-contract")
        createProcess("doc-fin-request-1", "fin-request")
        createProcess("doc-meeting-1", "meeting-activity")
    }

    @AfterAll
    fun tearDown() {
        startedProcessIds.forEach { processId ->
            bpmnProcessService.deleteProcessInstance(processId)
        }
        helper.clearTasks()
    }

    @Test
    fun `filter by documentTypeRef should return only matching tasks`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(
                        ProcTaskSqlQueryBuilder.ATT_DOCUMENT_TYPE_REF,
                        "emodel/type@ecos-contract"
                    )
                )
            )
        }

        assertThat(found).hasSize(2)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactlyInAnyOrder("doc-contract-1", "doc-contract-2")
    }

    @Test
    fun `filter by documentType should return only matching tasks`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_DOCUMENT_TYPE, "fin-request")
                )
            )
        }

        assertThat(found).hasSize(1)

        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc-fin-request-1")
    }

    @Test
    fun `without documentTypeRef filter should return all tasks`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
            )
        }

        assertThat(found).hasSize(4)
    }

    private fun createProcess(docId: String, documentType: String) {
        val docRef = EntityRef.valueOf("eproc/$documentType@$docId")

        val processInstance = bpmnProcessService.startProcess(
            StartProcessRequest(
                "",
                PROC_ID,
                docRef.toString(),
                mapOf(
                    BPMN_DOCUMENT_REF to docRef.toString(),
                    BPMN_DOCUMENT_TYPE to documentType
                )
            )
        )

        startedProcessIds.add(processInstance.id)
    }

    private fun getDocIdsFromTasks(taskRefs: List<EntityRef>): List<String> {
        return recordsService.getAtts(taskRefs, mapOf("documentRef" to "documentRef?id"))
            .map { it.getAtt("documentRef").asText() }
            .map { EntityRef.valueOf(it).getLocalId() }
    }
}
