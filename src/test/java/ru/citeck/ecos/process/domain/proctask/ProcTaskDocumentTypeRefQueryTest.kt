package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.service.ATT_CURRENT_USER_WITH_AUTH
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcTaskDocumentTypeRefQueryTest {

    companion object {
        private const val TEST_USER = "testUser"
        private const val PROC_ID = "bpmn-task-query-document-atts-simple-task-create"
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var camundaTaskService: TaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    private val startedProcessIds = mutableListOf<String>()

    @BeforeAll
    fun setUp() {
        helper.clearTasks()

        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )

        createProcessWithDocumentType("doc1", "contract")
        createProcessWithDocumentType("doc2", "invoice")
        createProcessWithDocumentType("doc3", "report")
    }

    @AfterAll
    fun tearDown() {
        startedProcessIds.forEach { processId ->
            bpmnProcessService.deleteProcessInstance(processId)
        }
        helper.clearTasks()
    }

    @Test
    fun `documentTypeRef eq filter with explicit predicate language`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq("documentTypeRef", "emodel/type@contract")
                )
            )
        }

        assertThat(found).hasSize(1)
        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc1")
    }

    @Test
    fun `documentTypeRef eq filter with different type`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq("documentTypeRef", "emodel/type@invoice")
                )
            )
        }

        assertThat(found).hasSize(1)
        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc2")
    }

    @Test
    fun `documentTypeRef contains is converted to eq`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            helper.queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.contains("documentTypeRef", "emodel/type@contract")
                )
            )
        }

        assertThat(found).hasSize(1)
        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc1")
    }

    @Test
    fun `predicate auto-detection when language is empty`() {
        val found = AuthContext.runAsFull(TEST_USER) {
            val query = RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage("")
                withPage(QueryPage(10_000, 0, null))
            }
            query.copy()
                .withQuery(
                    DataValue.create(
                        """{"t":"and","val":[
                        {"t":"eq","att":"actor","val":"$ATT_CURRENT_USER_WITH_AUTH"},
                        {"t":"eq","att":"documentTypeRef","val":"emodel/type@contract"}
                    ]}"""
                    )
                ).build().let { recordsService.query(it).getRecords() }
        }

        assertThat(found).hasSize(1)
        val docIds = getDocIdsFromTasks(found)
        assertThat(docIds).containsExactly("doc1")
    }

    @Test
    fun `empty language with no predicate falls back to alwaysTrue`() {
        // VoidPredicate (alwaysTrue) returns empty for non-admin users
        val found = AuthContext.runAsFull(TEST_USER) {
            val query = RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage("")
                withQuery(DataValue.createObj())
                withPage(QueryPage(10_000, 0, null))
            }
            recordsService.query(query).getRecords()
        }
        assertThat(found).isEmpty()

        // Admin users get all tasks with alwaysTrue predicate
        val adminFound = AuthContext.runAsSystem {
            val query = RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage("")
                withQuery(DataValue.createObj())
                withPage(QueryPage(10_000, 0, null))
            }
            recordsService.query(query).getRecords()
        }
        assertThat(adminFound).hasSize(3)
    }

    private fun createProcessWithDocumentType(docId: String, documentType: String) {
        val docRef = EntityRef.valueOf("eproc/test-doc@$docId")

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
