package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

/**
 * Asserts the shape of the generated SQL, not its results.
 *
 * Rendering variable conditions as a LEFT JOIN plus "alias.id_ IS [NOT] NULL", or splitting the
 * document condition into "EXISTS(documentRef) OR EXISTS(mainDocumentRef)", returns exactly the
 * same rows — so every behavioural test stays green — while restoring the query plan that made
 * the task-form request take ~2.5s in production instead of ~0.2ms. These assertions are the
 * only thing pinning that down.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskSqlQueryShapeTest {

    @Autowired
    private lateinit var authoritiesApi: EcosAuthoritiesApi

    @Autowired
    private lateinit var camundaTaskService: TaskService

    @Autowired
    private lateinit var procTaskAttsSyncService: ProcTaskAttsSyncService

    @Autowired
    private lateinit var processEngineConfiguration: ProcessEngineConfigurationImpl

    private fun newBuilder() = ProcTaskSqlQueryBuilder(
        authoritiesApi,
        camundaTaskService,
        procTaskAttsSyncService,
        processEngineConfiguration
    )

    private fun buildSql(predicate: Predicate): String {
        return newBuilder()
            .addConditions(predicate)
            .buildTaskSql("DISTINCT $TASK_ID_FIELD", withLimitAndSort = true)
    }

    @Test
    fun `document condition is rendered as a single semi join over both variable names`() {
        val sql = buildSql(
            Predicates.eq(ProcTaskSqlQueryBuilder.ATT_DOCUMENT, "eproc/test-doc@1")
        )

        // one EXISTS covering both names — NOT "EXISTS(a) OR EXISTS(b)", which PostgreSQL
        // cannot flatten into a semi-join and would evaluate per act_ru_task row
        assertThat(sql).contains("EXISTS (SELECT 1 FROM act_ru_variable")
        assertThat(sql).containsPattern("name_ IN \\(#\\{p\\d+},#\\{p\\d+}\\)")
        assertThat(countOccurrences(sql, "EXISTS (SELECT 1 FROM act_ru_variable")).isEqualTo(1)
        // "EXISTS (" is a substring of "NOT EXISTS (", so the polarity has to be asserted separately
        assertThat(sql).doesNotContain("NOT EXISTS")
        assertThat(sql).doesNotContain("LEFT JOIN act_ru_variable")
    }

    @Test
    fun `synced attribute condition is rendered as a semi join`() {
        val sql = buildSql(Predicates.eq("_doc_documentStatus", "NEW"))

        assertThat(sql).contains("EXISTS (SELECT 1 FROM act_ru_variable")
        assertThat(sql).doesNotContain("NOT EXISTS")
        assertThat(sql).doesNotContain("LEFT JOIN")
    }

    @Test
    fun `empty predicate on synced attribute is rendered as an anti join`() {
        val sql = buildSql(Predicates.empty("_doc_documentStatus"))

        assertThat(sql).contains("NOT EXISTS (SELECT 1 FROM act_ru_variable")
        assertThat(sql).doesNotContain("LEFT JOIN")
    }

    @Test
    fun `variable names are bound as parameters and never inlined into sql`() {
        val sql = buildSql(Predicates.eq("_doc_status'; DROP TABLE act_ru_task; --", "NEW"))

        assertThat(sql).doesNotContain("DROP TABLE")
        assertThat(sql).containsPattern("name_ = #\\{p\\d+}")
    }

    @Test
    fun `no placeholder leaks into the generated sql`() {
        val sql = buildSql(
            Predicates.and(
                Predicates.eq(ProcTaskSqlQueryBuilder.ATT_DOCUMENT, "eproc/test-doc@1"),
                Predicates.eq("_doc_documentStatus", "NEW"),
                Predicates.not(Predicates.empty("_doc_documentType"))
            )
        )

        assertThat(sql).doesNotContain("@@VAR_COND_")
    }

    private fun countOccurrences(text: String, part: String): Int {
        var count = 0
        var idx = text.indexOf(part)
        while (idx >= 0) {
            count++
            idx = text.indexOf(part, idx + part.length)
        }
        return count
    }

    companion object {
        private const val TASK_ID_FIELD = "task.ID_"
    }
}
