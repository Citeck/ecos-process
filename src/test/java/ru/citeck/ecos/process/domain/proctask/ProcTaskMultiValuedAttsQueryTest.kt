package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.process.domain.withDocPrefix
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

/**
 * Queries over multi-valued (multiple = true) synced task attributes.
 *
 * Such attributes are stored in act_ge_bytearray as a JSON list and are matched with LIKE,
 * which is a separate branch of the query builder from the scalar act_ru_variable one.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcTaskMultiValuedAttsQueryTest {

    companion object {
        private const val PROC_ID_SIMPLE_TASK = "bpmn-task-atts-document-simple-task-create"
        private const val DOCUMENT_TYPE = "doc-multi-sync-query"

        private const val TAGS_ATT = "tags"

        private val docWithTags = DocWithTags()
        private val docWithSingleTag = DocWithSingleTag()
        private val docWithoutTags = DocWithoutTags()
    }

    private val startedProcessIds = mutableListOf<String>()

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var ecosTypeRegistry: EcosTypesRegistry

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var authoritiesApi: EcosAuthoritiesApi

    @Autowired
    private lateinit var camundaTaskService: TaskService

    @Autowired
    private lateinit var procTaskAttsSyncService: ProcTaskAttsSyncService

    @Autowired
    private lateinit var processEngineConfiguration: ProcessEngineConfigurationImpl

    private lateinit var documentRecordsDao: InMemRecordsDao<Any>

    @BeforeAll
    fun setUp() {
        helper.cleanTaskAttsSyncSettings()

        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID_SIMPLE_TASK.bpmn.xml",
            PROC_ID_SIMPLE_TASK,
            BpmnProcessDefActions.DEPLOY
        )

        documentRecordsDao = RecordsDaoBuilder.create("eproc/$DOCUMENT_TYPE")
            .addRecord(docWithTags.docRef.getLocalId(), docWithTags)
            .addRecord(docWithSingleTag.docRef.getLocalId(), docWithSingleTag)
            .addRecord(docWithoutTags.docRef.getLocalId(), docWithoutTags)
            .build()

        recordsService.register(documentRecordsDao)

        ecosTypeRegistry.setValue(
            DOCUMENT_TYPE,
            TypeDef.create()
                .withId(DOCUMENT_TYPE)
                .withModel(
                    TypeModelDef.create {
                        withAttributes(
                            listOf(
                                AttributeDef.create {
                                    withId(TAGS_ATT)
                                    withType(AttributeType.TEXT)
                                    withMultiple(true)
                                }
                            )
                        )
                    }
                )
                .build()
        )

        helper.createAttsSync(
            id = "test-task-atts-multi-sync-query",
            enabled = true,
            source = TaskAttsSyncSource.RECORD,
            name = "test-task-atts-multi-sync-query",
            attributesSync = listOf(
                TaskSyncAttribute(
                    id = TAGS_ATT,
                    type = AttributeType.TEXT,
                    multiple = true,
                    ecosTypes = listOf(
                        TaskSyncAttributeType(
                            typeRef = docWithTags.type,
                            attribute = TAGS_ATT
                        )
                    )
                )
            )
        )

        listOf(docWithTags.docRef, docWithSingleTag.docRef, docWithoutTags.docRef).forEach { docRef ->
            val instance = bpmnProcessService.startProcess(
                StartProcessRequest(
                    PROC_ID_SIMPLE_TASK,
                    docRef.toString(),
                    mapOf(
                        BPMN_DOCUMENT_REF to docRef.toString(),
                        BPMN_DOCUMENT_TYPE to DOCUMENT_TYPE
                    )
                )
            )
            startedProcessIds.add(instance.id)
        }
    }

    @AfterAll
    fun clean() {
        helper.cleanTaskAttsSyncSettings()
        startedProcessIds.forEach {
            bpmnProcessService.deleteProcessInstance(it)
        }
    }

    @Test
    fun `query multi valued attribute by exact value`() {
        val found = queryTasks(Predicates.eq(TAGS_ATT.withDocPrefix(), "alpha"))

        assertThat(found.map { it.docRef }).containsExactly(docWithTags.docRef)
    }

    @Test
    fun `query multi valued attribute by exact value should not match by prefix`() {
        // "alph" is a prefix of "alpha", but EQ on a list is quoted (%"alph"%) so it must not match
        assertThat(queryTasks(Predicates.eq(TAGS_ATT.withDocPrefix(), "alph"))).isEmpty()
    }

    @Test
    fun `query multi valued attribute by value of another document`() {
        val found = queryTasks(Predicates.eq(TAGS_ATT.withDocPrefix(), "gamma"))

        assertThat(found.map { it.docRef }).containsExactly(docWithSingleTag.docRef)
    }

    @Test
    fun `query multi valued attribute with contains`() {
        val found = queryTasks(Predicates.contains(TAGS_ATT.withDocPrefix(), "amm"))

        assertThat(found.map { it.docRef }).containsExactly(docWithSingleTag.docRef)
    }

    /**
     * Also covers a document without the attribute: it is never matched by a value predicate.
     * EMPTY is not asserted anywhere in this class on purpose — the synchronizer always writes a
     * row for a multi-valued attribute (an absent value becomes "[]"), so an EMPTY predicate on
     * such an attribute can never match. That is pre-existing synchronizer behaviour, unrelated
     * to how the condition is rendered in SQL.
     */
    @Test
    fun `query multi valued attribute by any of several values`() {
        val found = queryTasks(
            Predicates.inVals(TAGS_ATT.withDocPrefix(), listOf("beta", "gamma"))
        )

        assertThat(found.map { it.docRef })
            .containsExactlyInAnyOrder(docWithTags.docRef, docWithSingleTag.docRef)
    }

    /**
     * Shape assertion, not a behavioural one: reverting the multi-valued branch to
     * LEFT JOIN act_ge_bytearray returns exactly the same rows, so every test above stays green
     * while the query plan degrades. ProcTaskSqlQueryShapeTest cannot cover this branch because
     * it needs a registered multiple = true sync attribute, which only this fixture has.
     */
    @Test
    fun `multi valued attribute condition is rendered as a semi join over act_ge_bytearray`() {
        val sql = ProcTaskSqlQueryBuilder(
            authoritiesApi,
            camundaTaskService,
            procTaskAttsSyncService,
            processEngineConfiguration
        )
            .addConditions(Predicates.eq(TAGS_ATT.withDocPrefix(), "alpha"))
            .buildTaskSql("DISTINCT task.ID_", withLimitAndSort = false)

        assertThat(sql).contains("EXISTS (SELECT 1 FROM act_ge_bytearray")
        assertThat(sql).doesNotContain("NOT EXISTS")
        assertThat(sql).doesNotContain("LEFT JOIN")
    }

    private fun queryTasks(predicate: Predicate): List<TaskInfo> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withPage(QueryPage(10_000, 0, null))
            },
            TaskInfo::class.java
        ).getRecords()
    }

    data class TaskInfo(
        val id: String,

        @AttName("documentRef?id")
        val docRef: EntityRef
    )

    class DocWithTags(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@multi-1"),

        @AttName("tags")
        val tags: List<String> = listOf("alpha", "beta"),

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocWithSingleTag(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@multi-2"),

        @AttName("tags")
        val tags: List<String> = listOf("gamma"),

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )

    class DocWithoutTags(

        val docRef: EntityRef = EntityRef.valueOf("eproc/$DOCUMENT_TYPE@multi-3"),

        @AttName("_type")
        val type: EntityRef = EntityRef.valueOf("emodel/type@$DOCUMENT_TYPE")
    )
}
