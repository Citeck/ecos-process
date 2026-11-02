package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.apache.ibatis.builder.xml.XMLMapperBuilder
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.Configuration
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.test.commons.containers.TestContainers
import java.util.UUID

class BpmnProcessLatestFilterTest {

    private val schema = "picker_${UUID.randomUUID().toString().replace("-", "")}"
    private var schemaCreated = false

    private val dataSource by lazy {
        TestContainers.getPostgres(BpmnProcessLatestFilterTest::class) {
            releaseAfterTest(true)
        }.getDataSource()
    }

    private val factory by lazy {
        val config = Configuration(Environment("test", JdbcTransactionFactory(), dataSource)).also { conf ->
            javaClass.getResourceAsStream("/camunda/mybatis/bpmnProcessDefinitionMapper.xml").use {
                XMLMapperBuilder(it, conf, "bpmnProcessDefinitionMapper.xml", conf.sqlFragments).parse()
            }
        }

        SqlSessionFactoryBuilder().build(config)
    }

    @BeforeEach
    fun createSchema() {

        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE SCHEMA $schema")
                schemaCreated = true

                stmt.execute(
                    """
                    CREATE TABLE $schema.ACT_RE_PROCDEF (
                        ID_ VARCHAR PRIMARY KEY,
                        KEY_ VARCHAR,
                        VERSION_ INT,
                        NAME_ VARCHAR,
                        RESOURCE_NAME_ VARCHAR
                    )
                    """.trimIndent()
                )

                stmt.execute(
                    """
                    INSERT INTO $schema.ACT_RE_PROCDEF VALUES
                        ('a1', 'assignment-process', 1, NULL, 'assignment-process.bpmn'),
                        ('a2', 'assignment-process', 2, NULL, 'assignment-process.bpmn'),
                        ('b', 'other-process', 1, NULL, 'other.bpmn'),
                        ('c', 'CRM_100%', 1, NULL, 'crm.bpmn'),
                        ('d', 'ws0..assignment-process', 1, NULL, 'ws0..assignment-process.bpmn'),
                        ('e', 'ws1..assignment-process', 1, NULL, 'ws1..assignment-process.bpmn')
                    """.trimIndent()
                )
            }
        }
    }

    @AfterEach
    fun dropSchema() {

        if (schemaCreated) {
            dataSource.connection.use { conn ->
                conn.createStatement().use { it.execute("DROP SCHEMA $schema CASCADE") }
            }
        }
    }

    private fun query(
        predicate: Predicate,
        workspaces: List<String> = listOf(""),
        skip: Int = 0,
        max: Int = 100
    ): Pair<Long, List<ProcessDefinitionEntity>> {

        val filter = BpmnProcessLatestFilter.create(predicate) {
            if (it.getValue().asText() in listOf("Поручение", "assign")) {
                listOf("assignment-process.bpmn", "ws0..assignment-process.bpmn")
            } else {
                emptyList()
            }
        }

        val params = mapOf(
            "prefix" to "$schema.",
            "workspacesSysId" to workspaces,
            "filter" to filter,
            "skipCount" to skip,
            "maxItems" to max
        )

        return factory.openSession().use { session ->
            session.selectOne<Long>("selectCountOfLatestBpmnDefsByWsSysId", params) to
                session.selectList("selectLatestBpmnDefsByWsSysId", params)
        }
    }

    @Test
    fun filtersIdentifiersBeforeCountAndPaginationAndReturnsLatestVersion() {

        val (count, rows) = query(Predicates.contains("id", "ASSIGN"))
        assertEquals(1L, count)
        assertEquals(listOf("a2"), rows.map { it.id })
        assertEquals(1L, query(Predicates.eq("id", "assignment-process")).first)
        assertEquals(0L, query(Predicates.eq("id", "assignment")).first)
        assertEquals(0L, query(Predicates.contains("id", "not-found")).first)

        val (pageCount, page) = query(Predicates.contains("id", "process"), skip = 1, max = 1)
        assertEquals(2L, pageCount)
        assertEquals(1, page.size)
    }

    @Test
    fun supportsDefinitionSearchByKeyAndLocalizedNameWithinRequestedWorkspaces() {

        assertEquals(1L, query(Predicates.contains("definition", "assign")).first)
        assertEquals(1L, query(Predicates.contains("definition", "Поручение")).first)

        val (count, rows) = query(Predicates.contains("definition", "Поручение"), listOf("", "ws0"))
        assertEquals(2L, count)
        assertEquals(setOf("a2", "d"), rows.map { it.id }.toSet())

        val scopedRows = query(Predicates.contains("id", "assignment"), listOf("ws0")).second
        assertEquals(listOf("d"), scopedRows.map { it.id })
    }

    @Test
    fun preservesBooleanPredicatesAndDoesNotTreatLiteralWildcardsAsContainsPatterns() {

        val predicate = Predicates.and(
            Predicates.or(Predicates.contains("id", "assignment"), Predicates.contains("id", "other")),
            Predicates.not(Predicates.eq("id", "other-process"))
        )

        assertEquals(listOf("a2"), query(predicate).second.map { it.id })
        assertEquals(1L, query(Predicates.contains("id", "_100%")).first)
        assertEquals(0L, query(Predicates.contains("id", "' OR 1=1 --")).first)

        assertEquals(3L, query(Predicates.alwaysTrue()).first)
        assertEquals(3L, query(Predicates.eq("_type", "emodel/type@bpmn-proc-latest")).first)

        assertThrows(IllegalStateException::class.java) {
            BpmnProcessLatestFilter.create(Predicates.eq("unknown", "x"))
        }
    }
}
