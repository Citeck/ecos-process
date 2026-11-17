package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.IdentityService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.context.Context
import org.camunda.bpm.engine.impl.history.HistoryLevel
import org.camunda.bpm.engine.impl.persistence.entity.HistoricTaskInstanceEntity
import org.camunda.bpm.engine.impl.util.ClockUtil
import org.camunda.bpm.engine.variable.Variables
import org.camunda.bpm.model.bpmn.Bpmn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.context.lib.time.TimeZoneContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.api.records.ProcHistoricTaskRecords
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.config.PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.HistoricTaskQueryBuilder
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/** Real engine/PostgreSQL; no live document DAO is installed for the stored snapshot fixtures. */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcHistoricTaskFilteringTest {
    @Autowired private lateinit var caches: CacheManager
    @Autowired private lateinit var configuration: ProcessEngineConfigurationImpl
    @Autowired private lateinit var tasks: TaskService
    @Autowired private lateinit var runtime: RuntimeService
    @Autowired private lateinit var repository: RepositoryService
    @Autowired private lateinit var history: HistoryService
    @Autowired private lateinit var identity: IdentityService
    @Autowired private lateinit var historicRecords: ProcHistoricTaskRecords
    @Autowired private lateinit var historicService: ProcHistoricTaskService
    @Autowired private lateinit var currentService: ProcTaskService
    @Autowired private lateinit var records: RecordsService
    @Autowired private lateinit var helper: BpmnProcHelper
    @Autowired private lateinit var types: EcosTypesRegistry
    @Autowired private lateinit var sync: ProcTaskAttsSyncService
    @Autowired private lateinit var authoritiesApi: EcosAuthoritiesApi
    private var documentUnavailable = false

    private val deployments = mutableListOf<String>()
    private lateinit var jdbc: JdbcTemplate
    private val key = "historic-filter-test"
    private val doc = "eproc/historic-doc@old"
    private val stamp = Instant.parse("2026-10-01T12:00:00Z")

    @BeforeEach
    fun setup() {
        jdbc = JdbcTemplate(configuration.dataSource)
        records.register(object : AbstractRecordsDao(), RecordsAttsDao {
            override fun getId() = "historic-doc"
            override fun getRecordsAtts(recordIds: List<String>): List<*> {
                check(!documentUnavailable) { "Live document is unavailable" }
                return recordIds.map { FixtureDocument() }
            }
        })
        AuthContext.runAsSystem {
            types.setValue("saved-type", TypeDef.create().withId("saved-type").build())
            helper.cleanTaskAttsSyncSettings()
            helper.createAttsSync(
                id = "historic-filter-sync",
                enabled = true,
                source = TaskAttsSyncSource.RECORD,
                name = "Historic filtering",
                attributesSync = listOf(
                    TaskSyncAttribute("text", AttributeType.TEXT),
                    TaskSyncAttribute("number", AttributeType.NUMBER),
                    TaskSyncAttribute("date", AttributeType.DATETIME),
                    TaskSyncAttribute("calendarDate", AttributeType.DATE),
                    TaskSyncAttribute("bool", AttributeType.BOOLEAN),
                    TaskSyncAttribute("ref", AttributeType.ASSOC),
                    TaskSyncAttribute("person", AttributeType.PERSON),
                    TaskSyncAttribute("authority", AttributeType.AUTHORITY),
                    TaskSyncAttribute("group", AttributeType.AUTHORITY_GROUP),
                    TaskSyncAttribute("people", AttributeType.PERSON, true),
                    TaskSyncAttribute("authorities", AttributeType.AUTHORITY, true),
                    TaskSyncAttribute("groups", AttributeType.AUTHORITY_GROUP, true),
                    TaskSyncAttribute("tags", AttributeType.TEXT, true),
                    TaskSyncAttribute("refs", AttributeType.ASSOC, true),
                    TaskSyncAttribute("numbers", AttributeType.NUMBER, true),
                    TaskSyncAttribute("dates", AttributeType.DATE, true),
                    TaskSyncAttribute("bools", AttributeType.BOOLEAN, true),
                    TaskSyncAttribute("documentStatus", AttributeType.TEXT)
                )
            )
            deploy(key)
        }
    }

    private fun deploy(processKey: String) {
        deployments += repository.createDeployment().addModelInstance(
            "$processKey.bpmn",
            Bpmn.createExecutableProcess(processKey).startEvent()
                .userTask("review").name("Review O'Brien %_ Юникод").endEvent().done()
        ).deploy().id
    }

    @AfterEach
    fun cleanup() {
        identity.clearAuthentication()
        ClockUtil.reset()
        AuthContext.runAsSystem {
            deployments.reversed().forEach { repository.deleteDeployment(it, true) }
            helper.cleanTaskAttsSyncSettings()
        }
    }

    private fun start(
        document: String? = doc,
        local: Map<String, Any?> = emptyMap(),
        processKey: String = key,
        processVars: Map<String, Any?> = emptyMap()
    ): String {
        val process = runtime.startProcessInstanceByKey(
            processKey,
            mapOf("documentRef" to document, "documentType" to "saved-type") + processVars
        )
        val task = tasks.createTaskQuery().processInstanceId(process.id).singleResult()
        tasks.setAssignee(task.id, "assigned")
        tasks.setPriority(task.id, 42)
        tasks.setDueDate(task.id, Date.from(stamp))
        if (local.isNotEmpty()) tasks.setVariablesLocal(task.id, local)
        return task.id
    }

    private fun finish(id: String, user: String? = "alice") {
        if (user != null) tasks.setVariableLocal(id, "completedBy", user)
        tasks.complete(id)
    }

    private fun query(
        predicate: Predicate = Predicates.alwaysTrue(),
        max: Int = 100,
        skip: Int = 0,
        sort: List<SortBy> = emptyList()
    ): RecsQueryRes<EntityRef> = historicRecords.queryRecords(
        RecordsQuery.create {
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
            withQuery(predicate)
            withPage(QueryPage(max, skip, null))
            withSortBy(sort)
        }
    )

    private fun ids(predicate: Predicate = Predicates.alwaysTrue()): List<String> = AuthContext.runAsSystem { query(predicate).getRecords().map { it.getLocalId() } }

    @Test
    fun `common scalar and process filters match before and after completion`() {
        val id = start(
            local = mapOf(
                "_doc_text" to "Saved text",
                "_doc_number" to 12.5,
                "_doc_date" to Date.from(stamp),
                "_doc_bool" to true,
                "_doc_ref" to doc,
                "_doc_person" to "assigned",
                "_doc_t_text" to "old type"
            )
        )
        val paired = listOf(
            Predicates.eq("document", doc), Predicates.eq("mainDocumentRef", "eproc/historic-doc@main"),
            Predicates.eq("documentType", "saved-type"), Predicates.eq("documentTypeRef", "emodel/type@saved-type"),
            Predicates.eq("taskKey", "review"), Predicates.contains("name", "o'brien"), Predicates.eq("assignee", "assigned"),
            ValuePredicate.ge("_created", tasks.createTaskQuery().taskId(id).singleResult().createTime.toInstant().toString()),
            Predicates.ge("priority", 40), ValuePredicate.eq("dueDate", stamp.toString()),
            Predicates.eq("_doc_text", "Saved text"), Predicates.eq("_doc_number", 12.5),
            ValuePredicate.eq("_doc_date", stamp.toString()), Predicates.eq("_doc_bool", true),
            Predicates.eq("_doc_ref", doc), Predicates.eq("_doc_person", "assigned"), Predicates.eq("_doc_t_text", "old type")
        )
        val process = tasks.createTaskQuery().taskId(id).singleResult().processInstanceId
        runtime.setVariable(process, "mainDocumentRef", "eproc/historic-doc@main")
        AuthContext.runAsSystem {
            paired.forEach { assertThat(currentService.findTasks(it).entities).describedAs(it.toString()).contains(id) }
        }
        finish(id)
        paired.forEach { assertThat(ids(it)).describedAs(it.toString()).containsExactly(id) }
        assertThat(ids(Predicates.eq("processInstanceId", "eproc/bpmn-proc@$process"))).containsExactly(id)
        val historic = history.createHistoricTaskInstanceQuery().taskId(id).singleResult()
        assertThat(ids(Predicates.eq("processDefinitionId", historic.processDefinitionId))).containsExactly(id)
        assertThat(ids(Predicates.contains("documentTypeRef", "emodel/type@saved-type"))).containsExactly(id)
    }

    @Test
    fun priorityJournalNumericSelectMatchesExactPriority() {
        assertPriorityJournalSelect(2)
    }

    @Test
    fun priorityJournalStringSelectMatchesExactPriority() {
        assertPriorityJournalSelect("2")
    }

    private fun assertPriorityJournalSelect(value: Any) {
        val byPriority = listOf(1, 2, 3, 12, 20, 42).associateWith { priority ->
            start().also { tasks.setPriority(it, priority) }
        }
        val selected = byPriority.getValue(2)
        val operations = listOf(ValuePredicate.Type.CONTAINS, ValuePredicate.Type.LIKE)
        AuthContext.runAsSystem {
            operations.forEach { op ->
                assertThat(currentService.findTasks(ValuePredicate("priority", op, value)).entities).containsExactly(selected)
            }
        }
        byPriority.values.forEach { finish(it) }
        val otherUser = start().also { tasks.setPriority(it, 2) }
        finish(otherUser, "bob")
        operations.forEach { op ->
            val predicate = ValuePredicate("priority", op, value)
            assertThat(ids(predicate)).containsExactlyInAnyOrder(selected, otherUser)
            assertThat(ids(Predicates.and(predicate, Predicates.eq("completedBy", "alice")))).containsExactly(selected)
            AuthContext.runAsFull("alice") {
                val journal = records.query(
                    RecordsQuery.create {
                        withSourceId("eproc/proc-historic-task")
                        withLanguage(PredicateService.LANGUAGE_PREDICATE)
                        withQuery(mapOf("t" to op.name.lowercase(), "a" to "priority", "v" to value))
                        withPage(QueryPage(20, 0, null))
                    },
                    HistoricJournalRow::class.java
                )
                assertThat(journal.getRecords().map { EntityRef.valueOf(it.id).getLocalId() }).containsExactly(selected)
                assertThat(journal.getTotalCount()).isEqualTo(1)
                val composed = Predicates.and(
                    Predicates.or(predicate, Predicates.eq("priority", 3)),
                    Predicates.not(Predicates.eq("priority", 3))
                )
                assertThat(query(composed).getRecords().map { it.getLocalId() }).containsExactly(selected)
                assertThat(query(Predicates.and(predicate, Predicates.eq("completedBy", "bob"))).getRecords()).isEmpty()
                assertThat(query(Predicates.not(predicate)).getRecords().map { it.getLocalId() })
                    .containsExactlyInAnyOrderElementsOf(byPriority.filterKeys { it != 2 }.values)
            }
        }
    }

    @Test
    fun priorityJournalTextOperationsRejectInvalidValuesAndSyncedNumbers() {
        for (op in listOf(ValuePredicate.Type.CONTAINS, ValuePredicate.Type.LIKE)) {
            for (invalid in listOf(null, "Средний", "2x", listOf(2), mapOf("value" to 2), true)) {
                assertThatThrownBy {
                    HistoricTaskQueryBuilder(configuration, sync, authoritiesApi)
                        .select(ValuePredicate("priority", op, invalid), emptyList(), 0, 20)
                }.isInstanceOf(IllegalArgumentException::class.java)
            }
            assertThatThrownBy {
                HistoricTaskQueryBuilder(configuration, sync, authoritiesApi)
                    .select(ValuePredicate("_doc_number", op, 2), emptyList(), 0, 20)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Unsupported historic task text operation $op for NUMBER")
        }
    }

    @Test
    fun `recursive predicates preserve repeated field intersection and cross branch negation`() {
        val a = start(local = mapOf("_doc_text" to "alpha", "_doc_number" to 15.0))
        val b = start(document = "eproc/historic-doc@other", local = mapOf("_doc_text" to "beta", "_doc_number" to 30.0))
        finish(a)
        finish(b, "bob")
        assertThat(ids(Predicates.and(Predicates.eq("_doc_text", "alpha"), Predicates.eq("_doc_text", "beta")))).isEmpty()
        assertThat(ids(Predicates.and(Predicates.gt("_doc_number", 10), Predicates.lt("_doc_number", 20)))).containsExactly(a)
        val nested = Predicates.or(
            Predicates.and(Predicates.eq("document", doc), Predicates.not(Predicates.eq("_doc_text", "beta"))),
            Predicates.and(Predicates.eq("completedBy", "bob"), Predicates.lt("priority", 0))
        )
        assertThat(ids(nested)).containsExactly(a)
        assertThat(ids(Predicates.not(Predicates.or(Predicates.eq("document", doc), Predicates.eq("_doc_text", "beta"))))).isEmpty()
    }

    @Test
    fun `empty IN null missing and nullable columns have two valued leaf semantics`() {
        val a = start(null)
        tasks.setDueDate(a, null)
        val b = start(local = mapOf("_doc_text" to "", "_doc_tags" to arrayListOf<String>()))
        val c = start(local = mapOf("_doc_text" to "nonempty"))
        finish(a)
        finish(b)
        finish(c)
        assertThat(ids(Predicates.inVals("priority", emptyList<Any>()))).isEmpty()
        assertThat(ids(Predicates.not(Predicates.inVals("_doc_text", emptyList<Any>())))).containsExactlyInAnyOrder(a, b, c)
        assertThat(ids(Predicates.empty("_doc_text"))).containsExactlyInAnyOrder(a, b)
        assertThat(ids(Predicates.notEmpty("_doc_text"))).containsExactly(c)
        assertThat(ids(Predicates.eq("_doc_text", null))).containsExactlyInAnyOrder(a, b)
        assertThat(ids(Predicates.inVals("_doc_text", listOf(null, "nonempty")))).containsExactlyInAnyOrder(a, b, c)
        assertThat(ids(Predicates.not(ValuePredicate.eq("dueDate", stamp.toString())))).containsExactly(a)
        assertThat(ids(Predicates.empty("_doc_tags"))).containsExactlyInAnyOrder(a, b, c)
    }

    @Test
    fun `document empty checks both own process references and ignores task shadowing`() {
        val a = start(null, processVars = mapOf("mainDocumentRef" to "eproc/historic-doc@main"))
        val b = start(null)
        tasks.setVariableLocal(a, "documentRef", "eproc/historic-doc@shadow")
        finish(a)
        finish(b)
        assertThat(ids(Predicates.empty("document"))).containsExactly(b)
        assertThat(ids(Predicates.eq("document", "eproc/historic-doc@main"))).containsExactly(a)
        assertThat(ids(Predicates.eq("document", "eproc/historic-doc@shadow"))).isEmpty()
        assertThat(historicService.getHistoricTaskById(a)?.documentRef).isEqualTo(EntityRef.EMPTY)
    }

    @Test
    fun `user and administrator cannot bypass external completedBy guard and system can explicitly select another user`() {
        val a = start()
        val b = start()
        val unrecorded = start()
        finish(a)
        finish(b, "bob")
        finish(unrecorded, null)
        for (authorities in listOf(emptyList(), listOf(AuthRole.ADMIN))) {
            AuthContext.runAsFull("alice", authorities) {
                assertThat(AuthContext.isRunAsSystem()).isFalse()
                assertThat(AuthContext.isRunAsAdmin()).isEqualTo(authorities.isNotEmpty())
                assertThat(query().getRecords().map { it.getLocalId() }).containsExactly(a)
                assertThat(query(Predicates.eq("completedBy", "bob")).getRecords()).isEmpty()
                assertThat(
                    query(Predicates.or(Predicates.eq("completedBy", "bob"), Predicates.alwaysTrue()))
                        .getRecords().map { it.getLocalId() }
                ).containsExactly(a)
                assertThat(query(Predicates.not(Predicates.eq("completedBy", "alice"))).getRecords()).isEmpty()
                assertThat(
                    query(Predicates.inVals("completedBy", listOf("$" + "CURRENT", "$" + "CURRENT_USER")))
                        .getRecords().map { it.getLocalId() }
                ).containsExactly(a)
            }
        }
        assertThat(ids()).containsExactlyInAnyOrder(a, b, unrecorded)
        assertThat(ids(Predicates.eq("completedBy", "bob"))).containsExactly(b)
        assertThat(ids(Predicates.eq("assignee", "assigned"))).containsExactlyInAnyOrder(a, b, unrecorded)
    }

    @Test
    fun `stored snapshots metadata assignee and action flags read without a live document`() {
        val a = start(
            local = mapOf(
                "_doc_text" to "saved",
                "_doc_t_text" to "saved type",
                "_doc_documentStatus" to "old-status",
                "taskComment" to "",
                "lastComment" to "previous comment",
                "nameMl" to mapOf("en" to "Saved name")
            )
        )
        finish(a)
        documentUnavailable = true
        val dto = historicService.getHistoricTaskById(a)!!
        assertThat(dto.assignee).isNotEqualTo(EntityRef.EMPTY)
        assertThat(dto.completedBy).isEqualTo("alice")
        assertThat(dto.documentType).isEqualTo("saved-type")
        assertThat(dto.comment).isEmpty()
        assertThat(dto.lastComment).isEqualTo("previous comment")
        assertThat(dto.name.get(Locale.ENGLISH)).isEqualTo("Saved name")
        val result = AuthContext.runAsFull("alice") {
            records.getAtts(
                EntityRef.create("eproc", "proc-historic-task", a),
                listOf(
                    "_doc_text", "_doc_t_text", "_doc_documentStatus", "completedBy", "deleteReason", "assignee?id",
                    "reassignable?bool", "claimable?bool", "unclaimable?bool", "assignable?bool"
                )
            )
        }
        assertThat(result.getAtt("_doc_text").asText()).isEqualTo("saved")
        assertThat(result.getAtt("_doc_t_text").asText()).isEqualTo("saved type")
        assertThat(result.getAtt("_doc_documentStatus").asText()).isEqualTo("old-status")
        assertThat(result.getAtt("completedBy").asText()).isEqualTo("alice")
        for (flag in listOf("reassignable", "claimable", "unclaimable", "assignable")) {
            assertThat(result.getAtt("$flag?bool").asBoolean()).isFalse()
        }
    }

    @Test
    fun `completed and cancelled tasks remain distinguishable while unfinished tasks never affect count`() {
        val completed = start()
        val cancelled = start()
        start()
        val process = tasks.createTaskQuery().taskId(cancelled).singleResult().processInstanceId
        finish(completed)
        runtime.deleteProcessInstance(process, "cancelled by fixture")
        assertThat(ids()).containsExactlyInAnyOrder(completed, cancelled)
        assertThat(ids(Predicates.eq("deleteReason", "cancelled by fixture"))).containsExactly(cancelled)
        assertThat(historicService.getHistoricTaskById(cancelled)?.deleteReason).isEqualTo("cancelled by fixture")
        AuthContext.runAsSystem { assertThat(query().getTotalCount()).isEqualTo(2) }
    }

    @Test
    fun `pagination uses one finished selection stable ties accurate count and explicit null order`() {
        ClockUtil.setCurrentTime(Date.from(stamp))
        val withEmptyDueDate = start()
        tasks.setDueDate(withEmptyDueDate, null)
        finish(withEmptyDueDate)
        val all = (listOf(withEmptyDueDate) + (1..3).map { start().also { finish(it) } }).sorted()
        tasks.setDueDate(start(), null)
        AuthContext.runAsSystem {
            val first = query(max = 2)
            val second = query(max = 2, skip = 2)
            assertThat(first.getRecords().map { it.getLocalId() }).containsExactlyElementsOf(all.take(2))
            assertThat(second.getRecords().map { it.getLocalId() }).containsExactlyElementsOf(all.drop(2))
            assertThat(first.getTotalCount()).isEqualTo(4)
            assertThat(first.getHasMore()).isTrue()
            assertThat(second.getHasMore()).isFalse()
            assertThat(query(skip = Int.MAX_VALUE).getTotalCount()).isEqualTo(4)
            assertThat(query(skip = Int.MAX_VALUE).getRecords()).isEmpty()
            assertThat(query(skip = Int.MAX_VALUE).getHasMore()).isFalse()
            assertThat(query(max = 0).getRecords()).isEmpty()
            assertThat(query(max = 0).getHasMore()).isTrue()
            assertThat(query(max = -1).getRecords()).hasSize(4)
            assertThat(query(sort = listOf(SortBy("priority", true))).getRecords()).hasSize(4)
            assertThat(query(sort = listOf(SortBy("dueDate", true))).getRecords().last().getLocalId()).isEqualTo(withEmptyDueDate)
        }
    }

    @Test
    fun `timestamp and epoch date boundaries retain strict inclusive and submillisecond semantics`() {
        ClockUtil.setCurrentTime(Date.from(stamp))
        val a = start(local = mapOf("_doc_date" to Date.from(stamp)))
        finish(a)
        ClockUtil.reset()
        for (field in listOf("_created", "ended", "dueDate", "_doc_date")) {
            for ((op, expected) in listOf(
                ValuePredicate.Type.GT to false,
                ValuePredicate.Type.GE to true,
                ValuePredicate.Type.LT to false,
                ValuePredicate.Type.LE to true,
                ValuePredicate.Type.EQ to true
            )) {
                assertThat(ids(ValuePredicate(field, op, stamp.toString())).contains(a)).describedAs("$field $op").isEqualTo(expected)
            }
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.LT, stamp.plusNanos(500_000).toString()))).containsExactly(a)
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.GE, stamp.plusNanos(500_000).toString()))).isEmpty()
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.LT, stamp.plusNanos(1).toString()))).containsExactly(a)
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.GE, stamp.plusNanos(1).toString()))).isEmpty()
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.EQ, stamp.plusNanos(1).toString()))).isEmpty()
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.GT, stamp.minusNanos(1).toString()))).containsExactly(a)
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.LE, stamp.minusNanos(1).toString()))).isEmpty()
            assertThat(ids(ValuePredicate(field, ValuePredicate.Type.GE, stamp.toEpochMilli().toBigDecimal() + java.math.BigDecimal("0.5")))).isEmpty()
        }
        assertThat(ids(ValuePredicate.eq("dueDate", "2026-10-01T00:00:00Z/2026-10-02T00:00:00Z"))).containsExactly(a)
        assertThat(ids(ValuePredicate.eq("dueDate", "$" + "TODAY"))).isEmpty()
    }

    @Test
    fun relativeDueDateFiltersMatchSavedHistory() {
        assertRelativeDateFilters("dueDate", AttributeType.DATETIME)
    }

    @Test
    fun relativeDateSyncFiltersMatchSavedHistory() {
        assertRelativeDateFilters("_doc_calendarDate", AttributeType.DATE)
    }

    @Test
    fun relativeDateTimeSyncFiltersMatchSavedHistory() {
        assertRelativeDateFilters("_doc_date", AttributeType.DATETIME)
    }

    private fun assertRelativeDateFilters(field: String, type: AttributeType) {
        val previousOffset = TimeZoneContext.getUtcOffset()
        try {
            TimeZoneContext.doWithUtcOffset(Duration.ZERO) {
                // DbDateUtils uses Instant.now(), not Camunda ClockUtil; capture UTC today, with fixtures at noon.
                val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
                val todayNoon = todayStart.plus(12, ChronoUnit.HOURS)
                val tomorrowStart = todayStart.plus(1, ChronoUnit.DAYS)
                assertThat(TimeZoneContext.getUtcOffset()).isEqualTo(Duration.ZERO)
                assertThat(sync.getTaskAttTypeOrTextDefault(field)).isEqualTo(type)
                if (field != "dueDate") assertThat(sync.getTaskSyncAttribute(field)!!.multiple).isFalse()

                fun at(instant: Instant): String {
                    val local = if (field == "dueDate") emptyMap() else mapOf(field to Date.from(instant))
                    return start(local = local).also { tasks.setDueDate(it, Date.from(instant)) }
                }

                val today = at(todayNoon)
                val yesterday = at(todayNoon.minus(1, ChronoUnit.DAYS))
                val unrelated = at(todayNoon.minus(4, ChronoUnit.DAYS))
                val exclusiveEnd = at(tomorrowStart)
                val otherUser = at(todayNoon)
                val todayIds = listOf(today, otherUser)
                fun withOffset(instant: Instant): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atOffset(ZoneOffset.ofHours(3)))
                val offsetNoon = withOffset(todayNoon)
                val offsetRange = "${withOffset(todayStart)}/${withOffset(tomorrowStart)}"
                val paired = listOf(
                    ValuePredicate.eq(field, "\$TODAY") to todayIds,
                    ValuePredicate.eq(field, "-P2D/P1D") to (todayIds + yesterday),
                    ValuePredicate.eq(field, "-P2D/$todayStart") to listOf(yesterday),
                    ValuePredicate.eq(field, offsetNoon) to todayIds,
                    ValuePredicate.eq(field, todayNoon.toString()) to todayIds,
                    ValuePredicate.eq(field, offsetRange) to todayIds,
                    ValuePredicate.eq(field, "$todayStart/$tomorrowStart") to todayIds
                )
                AuthContext.runAsSystem {
                    paired.forEach { (predicate, expected) ->
                        assertThat(currentService.findTasks(predicate).entities).describedAs("current $predicate")
                            .containsExactlyInAnyOrderElementsOf(expected)
                    }
                }
                listOf(today, yesterday, unrelated, exclusiveEnd).forEach { finish(it) }
                finish(otherUser, "bob")
                if (field != "dueDate") {
                    assertThat(jdbc.queryForMap("SELECT VAR_TYPE_, LONG_ FROM ACT_HI_VARINST WHERE TASK_ID_ = ? AND NAME_ = ?", today, field))
                        .containsEntry("var_type_", "date").containsEntry("long_", todayNoon.toEpochMilli())
                }
                paired.forEach { (predicate, expected) ->
                    assertThat(ids(predicate)).describedAs("historic $predicate").containsExactlyInAnyOrderElementsOf(expected)
                }
                // Date IN is normalized per leaf in history; current service does not preprocess date arrays.
                val relativeIn = Predicates.inVals(field, listOf("\$TODAY", todayNoon.minus(1, ChronoUnit.DAYS).toString()))
                assertThat(ids(relativeIn)).containsExactlyInAnyOrder(today, yesterday, otherUser)
                AuthContext.runAsFull("alice") {
                    assertThat(query(ValuePredicate.eq(field, "\$TODAY")).getRecords().map { it.getLocalId() }).containsExactly(today)
                    assertThat(query(relativeIn).getRecords().map { it.getLocalId() }).containsExactlyInAnyOrder(today, yesterday)
                }
                assertThat(Instant.now().truncatedTo(ChronoUnit.DAYS)).describedAs("UTC day must not roll over during relative-date assertions")
                    .isEqualTo(todayStart)
            }
        } finally {
            assertThat(TimeZoneContext.getUtcOffset()).describedAs("scoped timezone restored even on failure").isEqualTo(previousOffset)
        }
    }

    @Test
    fun minutePrecisionDueDateFiltersMatchSavedHistory() {
        assertMinutePrecisionDateFilters("dueDate")
    }

    @Test
    fun minutePrecisionDateSyncFiltersMatchSavedHistory() {
        assertMinutePrecisionDateFilters("_doc_calendarDate")
    }

    @Test
    fun minutePrecisionDateTimeSyncFiltersMatchSavedHistory() {
        assertMinutePrecisionDateFilters("_doc_date")
    }

    private fun assertMinutePrecisionDateFilters(field: String) {
        val instant = Instant.parse("2026-11-16T12:00:00Z")
        fun at(value: Instant): String {
            val local = if (field == "dueDate") emptyMap() else mapOf(field to Date.from(value))
            return start(local = local).also { tasks.setDueDate(it, Date.from(value)) }
        }
        val exact = at(instant)
        val withinRange = at(instant.plusMillis(30_123))
        val before = at(instant.minusSeconds(60))
        val exclusiveEnd = at(instant.plusSeconds(60))
        val paired = listOf(
            ValuePredicate.eq(field, "2026-11-16T15:00+03:00") to listOf(exact),
            ValuePredicate.eq(field, "2026-11-16T12:00Z") to listOf(exact),
            ValuePredicate.eq(field, instant.toString()) to listOf(exact),
            ValuePredicate.eq(field, "2026-11-16T15:00+03:00/2026-11-16T15:01+03:00") to listOf(exact, withinRange),
            ValuePredicate.eq(field, "2026-11-16T12:00Z/2026-11-16T12:01Z") to listOf(exact, withinRange)
        )
        AuthContext.runAsSystem {
            paired.forEach { (predicate, expected) ->
                assertThat(currentService.findTasks(predicate).entities).describedAs("current $predicate")
                    .containsExactlyInAnyOrderElementsOf(expected)
            }
        }
        listOf(exact, withinRange, before, exclusiveEnd).forEach { finish(it) }
        paired.forEach { (predicate, expected) ->
            assertThat(ids(predicate)).describedAs("historic $predicate").containsExactlyInAnyOrderElementsOf(expected)
        }
        // Only history normalizes individual date IN members; no current date-array parity is claimed.
        assertThat(ids(Predicates.inVals(field, listOf("2026-11-16T15:00+03:00", "2026-11-16T12:01Z"))))
            .containsExactlyInAnyOrder(exact, exclusiveEnd)
        for (invalid in listOf("", " ", "not-a-date", "2026-11-16T15:00", "2026-11-16T15:61+03:00", "2026-11-16T15:00+19:00", "2026-11-16T15:00+03:99")) {
            assertThatThrownBy { ids(ValuePredicate.eq(field, invalid)) }.describedAs("$field rejects '$invalid'")
                .isInstanceOf(DateTimeParseException::class.java)
        }
    }

    @Test
    fun `serialized list exact search scope and literal JSON metacharacters use confirmed Spin storage`() {
        val tags = arrayListOf("alpha", "quote\" Юникод", "literal%_\\end")
        val a = start(local = mapOf("_doc_tags" to tags, "_doc_refs" to arrayListOf(doc)))
        val b = start(local = mapOf("_doc_tags" to arrayListOf("beta")))
        val paired = listOf(Predicates.eq("_doc_tags", "alpha"), Predicates.eq("_doc_refs", doc), Predicates.contains("_doc_tags", "alp"))
        AuthContext.runAsSystem {
            paired.forEach { assertThat(currentService.findTasks(it).entities).describedAs(it.toString()).contains(a).doesNotContain(b) }
        }
        finish(a)
        finish(b)
        assertThat(jdbc.queryForMap("SELECT VAR_TYPE_, TEXT2_, BYTEARRAY_ID_ FROM ACT_HI_VARINST WHERE TASK_ID_ = ? AND NAME_ = '_doc_tags'", a))
            .containsEntry("var_type_", "spin://application/json")
        tags.forEach { assertThat(ids(Predicates.eq("_doc_tags", it))).containsExactly(a) }
        assertThat(ids(Predicates.contains("_doc_tags", "юНИКОД"))).containsExactly(a)
        assertThat(ids(Predicates.contains("_doc_tags", "юникод"))).containsExactly(a)
        paired.forEach { assertThat(ids(it)).containsExactly(a) }
        assertThat(ids(Predicates.eq("_doc_tags", "alph"))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_tags", "al%"))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_tags", "al_ha"))).isEmpty()
        assertThat(ids(Predicates.inVals("_doc_tags", listOf("alpha", "beta")))).containsExactlyInAnyOrder(a, b)
        assertThat(historicService.getHistoricTaskById(a)?.historicVariables?.get("_doc_tags")).isEqualTo(tags)
    }

    @Test
    fun `typed scalar variables reject incompatible storage and numeric boolean date list exact operations`() {
        val a = start(
            local = mapOf(
                "_doc_number" to "12.5",
                "_doc_bool" to "true",
                "_doc_date" to stamp.toString(),
                "_doc_numbers" to arrayListOf(12.5),
                "_doc_dates" to arrayListOf("2026-10-01"),
                "_doc_bools" to arrayListOf(true)
            )
        )
        finish(a)
        assertThat(ids(Predicates.eq("_doc_number", 12.5))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_bool", true))).isEmpty()
        assertThat(ids(Predicates.empty("_doc_number"))).isEmpty()
        assertThat(ids(Predicates.empty("_doc_bool"))).isEmpty()
        assertThat(ids(ValuePredicate.eq("_doc_date", stamp.toString()))).isEmpty()
        for ((field, search) in listOf("_doc_numbers" to "12.5", "_doc_dates" to "2026", "_doc_bools" to "true")) {
            assertThat(ids(Predicates.contains(field, search))).containsExactly(a)
            assertThatThrownBy { ids(Predicates.eq(field, search)) }.hasMessageContaining("Unsupported")
            assertThatThrownBy { ids(Predicates.inVals(field, listOf(search))) }.hasMessageContaining("Unsupported")
            assertThatThrownBy { ids(ValuePredicate.gt(field, search)) }.hasMessageContaining("Unsupported")
        }
    }

    @Test
    fun `unrelated non UTF8 byte arrays and Java serialized sync values are never decoded as JSON`() {
        val a = start(local = mapOf("_doc_tags" to arrayListOf("safe")))
        val b = start(
            local = mapOf(
                "_doc_tags" to Variables.objectValue(arrayListOf("unsafe"))
                    .serializationDataFormat(Variables.SerializationDataFormats.JAVA).create()
            )
        )
        finish(a)
        finish(b)
        TxnContext.doInTxn {
            jdbc.update("INSERT INTO ACT_GE_BYTEARRAY (ID_, REV_, NAME_, BYTES_) VALUES ('historic-binary', 1, 'deployment', ?)", byteArrayOf(-1, -2, 0))
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ACT_GE_BYTEARRAY WHERE ID_ = 'historic-binary'", Long::class.java)).isEqualTo(1)
        try {
            assertThat(ids(Predicates.eq("_doc_tags", "safe"))).containsExactly(a)
            assertThat(ids(Predicates.eq("_doc_tags", "unsafe"))).isEmpty()
            assertThat(historicService.getHistoricTaskById(b)?.historicVariables?.get("_doc_tags")).isNull()
        } finally {
            TxnContext.doInTxn { jdbc.update("DELETE FROM ACT_GE_BYTEARRAY WHERE ID_ = 'historic-binary'") }
        }
    }

    @Test
    fun `query then full entity and Records snapshot read share the original command context`() {
        val a = start(local = mapOf("_doc_text" to "saved"))
        finish(a)
        configuration.commandExecutorTxRequired.execute { context ->
            AuthContext.runAsSystem {
                assertThat(ids(Predicates.eq("_doc_text", "saved"))).containsExactly(a)
                assertThat(Context.getCommandContext()).isSameAs(context)
                val entity = history.createHistoricTaskInstanceQuery().taskId(a).singleResult() as HistoricTaskInstanceEntity
                assertThat(entity.processInstanceId).isNotBlank()
                assertThat(entity.taskDefinitionKey).isEqualTo("review")
                assertThat(entity.startTime).isNotNull()
                assertThat(entity.assignee).isEqualTo("assigned")
                val record = records.getAtts(EntityRef.create("eproc", "proc-historic-task", a), listOf("_doc_text", "completedBy"))
                assertThat(record.getAtt("_doc_text").asText()).isEqualTo("saved")
                assertThat(record.getAtt("completedBy").asText()).isEqualTo("alice")
                assertThat(Context.getCommandContext()).isSameAs(context)
            }
        }
    }

    @Test
    fun `configuration checks do not affect ECOS query or snapshot read but history none is empty`() {
        deployments += repository.createDeployment().tenantId("tenant-a").addModelInstance(
            "historic-tenant.bpmn",
            Bpmn.createExecutableProcess("historic-tenant").startEvent()
                .userTask("tenant-task").endEvent().done()
        ).deploy().id
        val process = runtime.createProcessInstanceByKey("historic-tenant").processDefinitionTenantId("tenant-a").execute()
        val a = tasks.createTaskQuery().processInstanceId(process.id).singleResult().id
        tasks.setVariableLocal(a, "_doc_text", "saved")
        finish(a)
        val authorization = configuration.isAuthorizationEnabled
        val tenantCheck = configuration.isTenantCheckEnabled
        val historyLevel = configuration.historyLevel
        try {
            for (auth in listOf(false, true)) {
                for (tenants in listOf(false, true)) {
                    for (authenticated in listOf(false, true)) {
                        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
                        configuration.isAuthorizationEnabled = auth
                        configuration.isTenantCheckEnabled = tenants
                        if (authenticated) identity.setAuthentication("engine-stranger", emptyList(), listOf("different-tenant")) else identity.clearAuthentication()
                        AuthContext.runAsFull("alice") {
                            assertThat(query(Predicates.eq("_doc_text", "saved")).getRecords().map { it.getLocalId() }).containsExactly(a)
                            assertThat(historicService.getHistoricTaskById(a)?.historicVariables?.get("_doc_text")).isEqualTo("saved")
                        }
                        AuthContext.runAsFull("bob") { assertThat(query().getRecords()).isEmpty() }
                        AuthContext.runAsFull("bob", listOf(AuthRole.ADMIN)) { assertThat(query().getRecords()).isEmpty() }
                        AuthContext.runAsFull("alice", listOf(AuthRole.ADMIN)) { assertThat(query().getRecords()).hasSize(1) }
                        AuthContext.runAsSystem { assertThat(query(Predicates.eq("completedBy", "alice")).getRecords()).hasSize(1) }
                    }
                }
            }
            configuration.historyLevel = HistoryLevel.HISTORY_LEVEL_NONE
            assertThat(ids()).isEmpty()
            assertThat(historicService.getHistoricTaskById(a)).isNull()
        } finally {
            identity.clearAuthentication()
            configuration.isAuthorizationEnabled = authorization
            configuration.isTenantCheckEnabled = tenantCheck
            configuration.historyLevel = historyLevel
        }
    }

    @Test
    fun `configured table prefix works end to end for scalar query full entity and snapshot loading`() {
        val a = start(local = mapOf("_doc_tags" to arrayListOf("prefix"), "_doc_text" to "saved"))
        finish(a)
        val oldPrefix = configuration.databaseTablePrefix
        TxnContext.doInTxn { jdbc.execute("CREATE SCHEMA historic_filter_prefix") }
        try {
            for (table in listOf("ACT_HI_TASKINST", "ACT_HI_VARINST", "ACT_GE_BYTEARRAY")) {
                TxnContext.doInTxn { jdbc.execute("CREATE VIEW historic_filter_prefix.$table AS SELECT * FROM $table") }
            }
            configuration.databaseTablePrefix = "historic_filter_prefix."
            assertThat(ids(Predicates.eq("_doc_tags", "prefix"))).containsExactly(a)
            val dto = historicService.getHistoricTaskById(a)!!
            assertThat(dto.historicVariables["_doc_text"]).isEqualTo("saved")
            assertThat(dto.historicVariables["_doc_tags"]).isEqualTo(listOf("prefix"))
            assertThat(records.getAtt(EntityRef.create("eproc", "proc-historic-task", a), "completedBy").asText()).isEqualTo("alice")
        } finally {
            configuration.databaseTablePrefix = oldPrefix
            TxnContext.doInTxn { jdbc.execute("DROP SCHEMA IF EXISTS historic_filter_prefix CASCADE") }
        }
    }

    @Test
    fun `completed call activity uses child process scope and bytearray reference rather than root instance`() {
        deployments += repository.createDeployment().addModelInstance(
            "historic-parent.bpmn",
            Bpmn.createExecutableProcess("historic-parent").startEvent()
                .callActivity("call").calledElement(key).endEvent().done()
        ).deploy().id
        val parent = runtime.startProcessInstanceByKey("historic-parent", mapOf("documentRef" to "eproc/historic-doc@root"))
        val child = tasks.createTaskQuery().list().single()
        runtime.setVariableLocal(child.processInstanceId, "documentRef", "eproc/historic-doc@child")
        runtime.setVariableLocal(child.processInstanceId, "mainDocumentRef", "eproc/historic-doc@root")
        tasks.setVariableLocal(child.id, "_doc_tags", arrayListOf("child-tag"))
        tasks.setVariableLocal(child.id, "documentRef", "eproc/historic-doc@shadow")
        finish(child.id)
        assertThat(runtime.createProcessInstanceQuery().processInstanceId(parent.id).count()).isZero()
        assertThat(ids(Predicates.eq("_doc_tags", "child-tag"))).containsExactly(child.id)
        assertThat(ids(Predicates.eq("processInstanceId", child.processInstanceId))).containsExactly(child.id)
        assertThat(ids(Predicates.eq("processInstanceId", parent.id))).isEmpty()
        assertThat(ids(Predicates.eq("document", "eproc/historic-doc@child"))).containsExactly(child.id)
        assertThat(ids(Predicates.eq("mainDocumentRef", "eproc/historic-doc@root"))).containsExactly(child.id)
        assertThat(historicService.getHistoricTaskById(child.id)?.documentRef?.toString()).isEqualTo("eproc/historic-doc@child")
    }

    @Test
    fun `last nonempty task comment is not substituted for subsequent empty task comment`() {
        deployments += repository.createDeployment().addModelInstance(
            "historic-comments.bpmn",
            Bpmn.createExecutableProcess("historic-comments").startEvent()
                .userTask("first").userTask("second").endEvent().done()
        ).deploy().id
        val process = runtime.startProcessInstanceByKey("historic-comments")
        val first = tasks.createTaskQuery().processInstanceId(process.id).singleResult()
        tasks.setVariableLocal(first.id, "taskComment", "first comment")
        finish(first.id)
        val second = tasks.createTaskQuery().processInstanceId(process.id).singleResult()
        tasks.setVariablesLocal(second.id, mapOf("taskComment" to "", "lastComment" to "first comment"))
        finish(second.id)
        assertThat(historicService.getHistoricTaskById(first.id)?.comment).isEqualTo("first comment")
        assertThat(historicService.getHistoricTaskById(second.id)?.comment).isEmpty()
    }

    @Test
    fun `text and reference IN remain parameterized across different definitions and authority placeholders`() {
        deploy("historic-other")
        val a = start(local = mapOf("_doc_person" to "alice", "_doc_authority" to "GROUP_reviewers"))
        val b = start(document = "eproc/historic-doc@other", processKey = "historic-other")
        finish(a)
        finish(b)
        assertThat(ids(Predicates.inVals("document", listOf(doc)))).containsExactly(a)
        assertThat(ids(Predicates.inVals("priority", listOf(42, 100)))).containsExactlyInAnyOrder(a, b)
        assertThat(ids(Predicates.inVals("taskKey", listOf("review", "absent")))).containsExactlyInAnyOrder(a, b)
        assertThat(ids(Predicates.contains("name", "o'brien"))).containsExactlyInAnyOrder(a, b)
        assertThat(ids(Predicates.eq("name", "' OR TRUE --"))).isEmpty()
        assertThat(ids(Predicates.eq("name", "Review O'Brien %_ Юникод"))).containsExactlyInAnyOrder(a, b)
        AuthContext.runAsFull("alice", listOf("GROUP_reviewers")) {
            assertThat(query(Predicates.eq("_doc_person", "$" + "CURRENT_USER")).getRecords().map { it.getLocalId() }).containsExactly(a)
            assertThat(
                query(Predicates.inVals("_doc_authority", listOf("absent", "$" + "CURRENT")))
                    .getRecords().map { it.getLocalId() }
            ).containsExactly(a)
        }
        AuthContext.runAsFull("assigned") {
            // Explicit assignee placeholder resolves to the login, not to the user's groups.
            assertThat(query(Predicates.eq("assignee", "$" + "CURRENT")).getRecords()).isEmpty()
        }
    }

    @Test
    fun `typed authority lists scalar groups references and placeholders retain field normalization`() {
        val a = start(
            local = mapOf(
                "_doc_people" to arrayListOf("alice"),
                "_doc_groups" to arrayListOf("GROUP_reviewers"),
                "_doc_authorities" to arrayListOf("alice", "GROUP_reviewers"),
                "_doc_group" to "GROUP_reviewers"
            )
        )
        val paired = listOf(
            Predicates.eq("_doc_people", authoritiesApi.getAuthorityRef("alice").toString()),
            Predicates.eq("_doc_groups", authoritiesApi.getAuthorityRef("GROUP_reviewers").toString()),
            Predicates.eq("_doc_authorities", "alice"),
            Predicates.eq("_doc_group", "GROUP_reviewers")
        )
        AuthContext.runAsSystem { paired.forEach { assertThat(currentService.findTasks(it).entities).contains(a) } }
        finish(a)
        paired.forEach { assertThat(ids(it)).containsExactly(a) }
        AuthContext.runAsFull("alice", listOf("GROUP_reviewers")) {
            assertThat(query(Predicates.inVals("_doc_people", listOf("$" + "CURRENT_USER"))).getRecords()).hasSize(1)
            assertThat(query(Predicates.eq("_doc_group", "$" + "CURRENT")).getRecords()).hasSize(1)
            assertThat(
                query(Predicates.inVals("completedBy", listOf(authoritiesApi.getAuthorityRef("alice").toString())))
                    .getRecords()
            ).hasSize(1)
        }
        val attrs = records.getAtts(EntityRef.create("eproc", "proc-historic-task", a), listOf("_doc_people[]?id", "_doc_group?id"))
        assertThat(attrs.getAtt("_doc_group?id").asText()).isEqualTo(authoritiesApi.getAuthorityRef("GROUP_reviewers").toString())
    }

    @Test
    fun `parallel tasks isolate snapshots and numeric native LONG storage is not compared through TEXT`() {
        deployments += repository.createDeployment().addModelInstance(
            "historic-parallel.bpmn",
            Bpmn.createExecutableProcess("historic-parallel").startEvent()
                .parallelGateway("fork").userTask("left").endEvent()
                .moveToNode("fork").userTask("right").endEvent().done()
        ).deploy().id
        val process = runtime.startProcessInstanceByKey("historic-parallel")
        val pair = tasks.createTaskQuery().processInstanceId(process.id).list()
        val a = pair.first { it.taskDefinitionKey == "left" }.id
        val b = pair.first { it.taskDefinitionKey == "right" }.id
        tasks.setVariablesLocal(a, mapOf("_doc_tags" to arrayListOf("left"), "_doc_number" to 42L, "_doc_bool" to false))
        tasks.setVariablesLocal(b, mapOf("_doc_tags" to arrayListOf("right"), "_doc_number" to 7.5, "_doc_bool" to true))
        finish(a)
        finish(b)
        assertThat(ids(Predicates.eq("_doc_tags", "left"))).containsExactly(a)
        assertThat(ids(Predicates.eq("_doc_number", 42))).containsExactly(a)
        assertThat(ids(Predicates.and(Predicates.ge("_doc_number", 40), Predicates.le("_doc_number", 42)))).containsExactly(a)
        assertThat(ids(Predicates.eq("_doc_bool", false))).containsExactly(a)
        assertThat(ids(Predicates.not(Predicates.eq("_doc_bool", true)))).containsExactly(a)
        assertThat(historicService.getHistoricTaskById(b)?.historicVariables?.get("_doc_tags")).isEqualTo(listOf("right"))
    }

    @Test
    fun `each allowed scalar operation and document aliases compose without losing repeated constraints`() {
        val a = start(local = mapOf("_doc_text" to "Alpha", "_doc_number" to 12.5))
        finish(a)
        for (field in listOf("name", "taskKey", "document", "mainDocumentRef", "deleteReason", "_doc_text")) {
            val empty = field == "mainDocumentRef"
            assertThat(ids(Predicates.empty(field)).contains(a)).describedAs(field).isEqualTo(empty)
            assertThat(ids(Predicates.notEmpty(field)).contains(a)).describedAs(field).isEqualTo(!empty)
        }
        for (op in listOf(ValuePredicate.Type.EQ, ValuePredicate.Type.GE, ValuePredicate.Type.LE)) {
            assertThat(ids(ValuePredicate("_doc_text", op, "Alpha"))).containsExactly(a)
        }
        assertThat(ids(Predicates.contains("_doc_text", "lPH"))).containsExactly(a)
        assertThat(ids(Predicates.and(Predicates.ge("priority", 42), Predicates.le("priority", 42)))).containsExactly(a)
        assertThat(ids(Predicates.inVals("documentType", listOf("absent", "emodel/type@saved-type")))).containsExactly(a)
        assertThat(ids(Predicates.alwaysFalse())).isEmpty()
        assertThat(ids(Predicates.not(Predicates.alwaysFalse()))).containsExactly(a)
        assertThat(ids(Predicates.and(emptyList()))).containsExactly(a)
        assertThat(ids(Predicates.or(emptyList()))).isEmpty()
    }

    @Test
    fun `unsupported fields operations values language and sorts fail explicitly`() {
        val predicates = listOf(
            Predicates.eq("actor", "alice"), Predicates.eq("actors", "alice"), Predicates.eq("actorManager", "alice"),
            Predicates.contains("documentType", null),
            Predicates.eq("unknown", "x"), Predicates.eq("_doc_unknown", "x"), Predicates.gt("documentType", 1),
            Predicates.eq("priority", "not a number"), Predicates.contains("_doc_number", "4"),
            Predicates.eq("_doc_bool", "maybe"), Predicates.eq("dueDate", "not a date"),
            Predicates.eq("processInstanceId", "other/process@1"), Predicates.eq("documentTypeRef", "other/type@1"),
            Predicates.contains("assignee", "assigned"), ValuePredicate.gt("name", null),
            Predicates.inVals("name", listOf(mapOf("injection" to "' OR TRUE --")))
        )
        predicates.forEach { predicate ->
            val failure = org.assertj.core.api.Assertions.catchThrowable { ids(predicate) }
            assertThat(failure).describedAs(predicate.toString()).isInstanceOf(Exception::class.java)
        }
        assertThatThrownBy {
            HistoricTaskQueryBuilder(configuration, sync, authoritiesApi).select(Predicates.alwaysTrue(), emptyList(), -1, 10)
        }.hasMessageContaining("skipCount")
        assertThatThrownBy { query(sort = listOf(SortBy("_doc_text", true))) }.hasMessageContaining("sort")
        assertThatThrownBy { historicRecords.queryRecords(RecordsQuery.create { withQuery(mapOf("unknown" to "ignored")) }) }
            .hasMessageContaining("predicate")
        assertThatThrownBy {
            historicRecords.queryRecords(
                RecordsQuery.create {
                    withLanguage("sql")
                    withQuery("1=1")
                }
            )
        }
            .hasMessageContaining("language")
    }

    @Test
    fun `review regression MLText binary codec preserves exact translations in cold DTO and Records`() {
        val translations = mapOf(Locale.ENGLISH to "Approve contract", Locale.forLanguageTag("ru") to "Согласовать договор")
        val title = MLText(translations)
        val a = start(local = mapOf("nameMl" to title))
        finish(a)
        val storedType = jdbc.queryForObject(
            "SELECT TEXT2_ FROM ACT_HI_VARINST WHERE TASK_ID_ = ? AND NAME_ = 'nameMl'",
            String::class.java,
            a
        )
        assertThat(storedType).isEqualTo(MLText::class.java.name)
        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
        val dto = historicService.getHistoricTaskById(a)!!
        assertThat(dto.name.getValues()).containsExactlyInAnyOrderEntriesOf(translations)
        val attrs = records.getAtts(EntityRef.create("eproc", "proc-historic-task", a), listOf("name?json"))
        assertThat(attrs.getAtt("name?json")["en"].asText()).isEqualTo(translations.getValue(Locale.ENGLISH))
        assertThat(attrs.getAtt("name?json")["ru"].asText()).isEqualTo(translations.getValue(Locale.forLanguageTag("ru")))
        for ((locale, translation) in translations) {
            caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
            I18nContext.doWithLocale(locale) {
                val localized = records.getAtts(EntityRef.create("eproc", "proc-historic-task", a), listOf("name", ".disp"))
                assertThat(localized.getAtt("name").asText()).describedAs("name in $locale").isEqualTo(translation)
                assertThat(localized.getAtt(".disp").asText()).describedAs(".disp in $locale").isEqualTo(translation)
            }
        }
    }

    @Test
    fun `review regression deleted scalar is empty and cannot match its old value`() {
        val a = start(local = mapOf("_doc_text" to "removed"))
        tasks.removeVariableLocal(a, "_doc_text")
        finish(a)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM ACT_HI_VARINST WHERE TASK_ID_ = ? AND NAME_ = '_doc_text' AND STATE_ = 'DELETED'",
                Long::class.java,
                a
            )
        ).isEqualTo(1)
        assertThat(ids(Predicates.empty("_doc_text"))).containsExactly(a)
        assertThat(ids(Predicates.eq("_doc_text", "removed"))).isEmpty()
        assertThat(historicService.getHistoricTaskById(a)!!.historicVariables).doesNotContainKey("_doc_text")
    }

    @Test
    fun `review regression recreated scalar excludes deleted instance from repeated constraints and reads`() {
        val a = start(local = mapOf("_doc_text" to "old scalar"))
        tasks.removeVariableLocal(a, "_doc_text")
        tasks.setVariableLocal(a, "_doc_text", "new scalar")
        finish(a)
        assertThat(ids(Predicates.and(Predicates.eq("_doc_text", "old scalar"), Predicates.eq("_doc_text", "new scalar")))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_text", "old scalar"))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_text", "new scalar"))).containsExactly(a)
        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
        assertThat(historicService.getHistoricTaskById(a)!!.historicVariables["_doc_text"]).isEqualTo("new scalar")
        assertThat(records.getAtt(EntityRef.create("eproc", "proc-historic-task", a), "_doc_text").asText()).isEqualTo("new scalar")
    }

    @Test
    fun `review regression recreated list excludes deleted elements and returns active snapshot`() {
        val a = start(local = mapOf("_doc_tags" to arrayListOf("old tag")))
        tasks.removeVariableLocal(a, "_doc_tags")
        tasks.setVariableLocal(a, "_doc_tags", arrayListOf("new tag"))
        finish(a)
        assertThat(ids(Predicates.eq("_doc_tags", "old tag"))).isEmpty()
        assertThat(ids(Predicates.eq("_doc_tags", "new tag"))).containsExactly(a)
        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
        assertThat(historicService.getHistoricTaskById(a)!!.historicVariables["_doc_tags"]).isEqualTo(listOf("new tag"))
    }

    @Test
    fun `review regression removed comment and process metadata are not returned`() {
        val a = start(local = mapOf("taskComment" to "removed comment"))
        val process = tasks.createTaskQuery().taskId(a).singleResult().processInstanceId
        tasks.removeVariableLocal(a, "taskComment")
        runtime.removeVariables(process, listOf("documentRef", "documentType"))
        finish(a)
        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
        val dto = historicService.getHistoricTaskById(a)!!
        assertThat(dto.comment).isNull()
        assertThat(dto.documentRef).isEqualTo(EntityRef.EMPTY)
        assertThat(dto.documentType).isNull()
        assertThat(dto.documentTypeRef).isEqualTo(EntityRef.EMPTY)
        assertThat(ids(Predicates.eq("document", doc))).isEmpty()
        assertThat(ids(Predicates.empty("document"))).containsExactly(a)
        assertThat(ids(Predicates.eq("documentType", "saved-type"))).isEmpty()
    }

    @Test
    fun `review regression deleted completedBy cannot satisfy explicit filter or ECOS actor guard`() {
        val a = start(local = mapOf("completedBy" to "alice"))
        tasks.removeVariableLocal(a, "completedBy")
        finish(a, null)
        assertThat(ids(Predicates.eq("completedBy", "alice"))).isEmpty()
        assertThat(ids()).containsExactly(a)
        for (authorities in listOf(emptyList(), listOf(AuthRole.ADMIN))) {
            AuthContext.runAsFull("alice", authorities) { assertThat(query().getRecords()).isEmpty() }
        }
        assertThat(historicService.getHistoricTaskById(a)!!.completedBy).isNull()
    }

    @Test
    fun `review regression NULL variable states remain queryable and readable`() {
        val a = start(local = mapOf("_doc_text" to "saved", "_doc_tags" to arrayListOf("tag"), "taskComment" to "comment"))
        val process = tasks.createTaskQuery().taskId(a).singleResult().processInstanceId
        finish(a)
        TxnContext.doInTxn {
            jdbc.update("UPDATE ACT_HI_VARINST SET STATE_ = NULL WHERE PROC_INST_ID_ = ?", process)
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ACT_HI_VARINST WHERE TASK_ID_ = ? AND STATE_ IS NULL", Long::class.java, a))
            .isGreaterThan(0)
        AuthContext.runAsFull("alice") {
            assertThat(query(Predicates.eq("_doc_text", "saved")).getRecords()).hasSize(1)
            assertThat(query(Predicates.eq("_doc_tags", "tag")).getRecords()).hasSize(1)
        }
        caches.getCache(PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY)!!.clear()
        val dto = historicService.getHistoricTaskById(a)!!
        assertThat(dto.historicVariables["_doc_text"]).isEqualTo("saved")
        assertThat(dto.historicVariables["_doc_tags"]).isEqualTo(listOf("tag"))
        assertThat(dto.documentRef.toString()).isEqualTo(doc)
        assertThat(dto.completedBy).isEqualTo("alice")
        assertThat(dto.comment).isEqualTo("comment")
    }

    private data class HistoricJournalRow(
        val id: String,
        @AttName("_created") val created: Instant?,
        val ended: Instant?,
        val completedBy: String?
    )

    private data class FixtureDocument(
        @AttName("_type") val type: EntityRef = EntityRef.valueOf("emodel/type@saved-type"),
        val name: String = "Live document"
    )

    @Test
    fun `journal ended order and root created filter remain compatible with user scope`() {
        val a = start()
        val b = start()
        val current = currentService.getTaskById(a)!!
        AuthContext.runAsFull("alice") {
            currentService.completeTask(CompleteTaskData(current, Outcome("review", "done", MLText("Done")), emptyMap()))
        }
        finish(b, "bob")
        val created = history.createHistoricTaskInstanceQuery().taskId(a).singleResult().startTime.toInstant()
        AuthContext.runAsFull("alice") {
            val result = query(ValuePredicate.gt("_created", created.minusSeconds(1).toString()), sort = listOf(SortBy("ended", false)))
            assertThat(result.getRecords().map { it.getLocalId() }).containsExactly(a)
            assertThat(result.getTotalCount()).isEqualTo(1)
            val journal = records.query(
                RecordsQuery.create {
                    withSourceId("eproc/proc-historic-task")
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(ValuePredicate.gt("_created", created.minusSeconds(1).toString()))
                    withSortBy(listOf(SortBy("ended", false)))
                    withPage(QueryPage(20, 0, null))
                },
                HistoricJournalRow::class.java
            )
            assertThat(journal.getRecords().map { EntityRef.valueOf(it.id).getLocalId() }).containsExactly(a)
            assertThat(journal.getRecords().single().created).isEqualTo(created)
            assertThat(journal.getRecords().single().ended).isNotNull()
            assertThat(journal.getRecords().single().completedBy).isEqualTo("alice")
        }
    }
}
