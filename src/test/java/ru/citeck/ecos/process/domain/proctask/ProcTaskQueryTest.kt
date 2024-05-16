package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.clearTasks
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.service.ATT_CURRENT_USER_WITH_AUTH
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_DUE_DATE
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_NAME
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder.Companion.ATT_PRIORITY
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.*

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskQueryTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var taskService: TaskService

    companion object {
        private const val HARRY_USER = "harry"
        private const val RON_USER = "ron"
        private const val VOLDEMORT_USER = "voldemort"
        private const val HERMIONE_USER = "hermione"

        private const val HOGWARTS_GROUP = "GROUP_hogwarts"
        private const val DEATH_EATERS_GROUP = "GROUP_death_eaters"
    }

    @BeforeEach
    fun setUp() {
        clearTasks()

        for (i in 1..7) {
            createTask(HARRY_USER)
        }
        for (i in 1..3) {
            createTask(candidateUsers = listOf(HARRY_USER))
        }
        for (i in 1..2) {
            createTask(
                assignee = HARRY_USER,
                candidateUsers = listOf(HARRY_USER),
                candidateGroups = listOf(HOGWARTS_GROUP)
            )
        }

        for (i in 1..12) {
            createTask(RON_USER)
        }
        for (i in 1..13) {
            createTask(candidateUsers = listOf(RON_USER))
        }
        for (i in 1..2) {
            createTask(
                assignee = RON_USER,
                candidateUsers = listOf(RON_USER),
                candidateGroups = listOf(HOGWARTS_GROUP)
            )
        }

        for (i in 1..5) {
            createTask(candidateGroups = listOf(HOGWARTS_GROUP))
        }

        for (i in 1..3) {
            createTask(VOLDEMORT_USER)
        }
        for (i in 1..7) {
            createTask(candidateUsers = listOf(VOLDEMORT_USER))
        }
        for (i in 1..2) {
            createTask(
                assignee = VOLDEMORT_USER,
                candidateUsers = listOf(VOLDEMORT_USER),
                candidateGroups = listOf(DEATH_EATERS_GROUP)
            )
        }

        for (i in 1..5) {
            createTask(candidateGroups = listOf(DEATH_EATERS_GROUP))
        }

        createTaskForFilterAndSort()
    }

    @AfterEach
    fun tearDown() {
        clearTasks()
    }

    @Test
    fun `should find all task for user harry`() {

        val found = AuthContext.runAsFull(HARRY_USER, listOf(HOGWARTS_GROUP)) {
            queryTasks(
                Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
            )
        }

        assertThat(found).hasSize(7 + 3 + 2 + 5)
    }

    @Test
    fun `should find all task for user ron`() {

        val found = AuthContext.runAsFull(RON_USER, listOf(HOGWARTS_GROUP)) {
            queryTasks(
                Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
            )
        }

        assertThat(found).hasSize(12 + 13 + 2 + 5)
    }

    @Test
    fun `should find all task for user voldemort`() {

        val found = AuthContext.runAsFull(VOLDEMORT_USER, listOf(DEATH_EATERS_GROUP)) {
            queryTasks(
                Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
            )
        }

        assertThat(found).hasSize(3 + 7 + 2 + 5)
    }

    @Test
    fun `should find task for assignee user harry`() {

        val found = AuthContext.runAsFull(HARRY_USER, listOf(HOGWARTS_GROUP)) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.notEmpty(ProcTaskSqlQueryBuilder.ATT_ASSIGNEE)
                )
            )
        }

        assertThat(found).hasSize(7 + 2)
    }

    @Test
    fun `should find task for group hogwarts exclude explicit assignee`() {

        val found = AuthContext.runAsFull("some-user", listOf(HOGWARTS_GROUP)) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.empty(ProcTaskSqlQueryBuilder.ATT_ASSIGNEE)
                )
            )
        }

        assertThat(found).hasSize(5)
    }

    @Test
    fun `query tasks with sort by priority desc`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
                ),
                SortBy(ATT_PRIORITY, false)
            )
        }

        assertThat(found).hasSize(4)
        assertThat(found[0].getLocalId()).isEqualTo("task4")
        assertThat(found[1].getLocalId()).isEqualTo("task3")
        assertThat(found[2].getLocalId()).isEqualTo("task2")
        assertThat(found[3].getLocalId()).isEqualTo("task1")
    }

    @Test
    fun `query tasks with sort by priority asc`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
                ),
                SortBy(ATT_PRIORITY, true)
            )
        }

        assertThat(found).hasSize(4)
        assertThat(found[0].getLocalId()).isEqualTo("task1")
        assertThat(found[1].getLocalId()).isEqualTo("task2")
        assertThat(found[2].getLocalId()).isEqualTo("task3")
        assertThat(found[3].getLocalId()).isEqualTo("task4")
    }

    @Test
    fun `query tasks with sort by due date desc`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
                ),
                SortBy(ATT_DUE_DATE, false)
            )
        }

        assertThat(found).hasSize(4)
        assertThat(found[0].getLocalId()).isEqualTo("task4")
        assertThat(found[1].getLocalId()).isEqualTo("task3")
        assertThat(found[2].getLocalId()).isEqualTo("task2")
        assertThat(found[3].getLocalId()).isEqualTo("task1")
    }

    @Test
    fun `query tasks with sort by due date asc`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH)
                ),
                SortBy(ATT_DUE_DATE, true)
            )
        }

        assertThat(found).hasSize(4)
        assertThat(found[0].getLocalId()).isEqualTo("task1")
        assertThat(found[1].getLocalId()).isEqualTo("task2")
        assertThat(found[2].getLocalId()).isEqualTo("task3")
        assertThat(found[3].getLocalId()).isEqualTo("task4")
    }

    @Test
    fun `filter tasks by priority`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ATT_PRIORITY, 2)
                )
            )
        }

        assertThat(found).hasSize(1)
        assertThat(found[0].getLocalId()).isEqualTo("task2")
    }

    @Test
    fun `filter tasks by due date`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ATT_DUE_DATE, Instant.parse("2021-01-02T15:00:00.0Z"))
                )
            )
        }

        assertThat(found).hasSize(1)
        assertThat(found[0].getLocalId()).isEqualTo("task2")
    }

    @Test
    fun `filter by due date le`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.le(ATT_DUE_DATE, Instant.parse("2021-01-02T00:00:00.0Z"))
                )
            )
        }

        assertThat(found).hasSize(1)
        assertThat(found[0].getLocalId()).isEqualTo("task1")
    }

    @Test
    fun `filter by due date ge`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.ge(ATT_DUE_DATE, Instant.parse("2021-01-02T00:00:00.0Z"))
                )
            )
        }

        assertThat(found).hasSize(2)
        assertThat(found.map { it.getLocalId() }).containsAll(listOf("task2", "task3"))
    }

    @Test
    fun `filter by due date is empty`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.empty(ATT_DUE_DATE)
                )
            )
        }

        assertThat(found).hasSize(1)
    }

    @Test
    fun `filter by due date is not empty`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.notEmpty(ATT_DUE_DATE)
                )
            )
        }

        assertThat(found).hasSize(3)
    }

    @Test
    fun `filter by due date is equals today`() {
        assertDoesNotThrow {
            val found = AuthContext.runAsFull(HERMIONE_USER) {
                queryTasks(
                    Predicates.and(
                        Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                        Predicates.eq(ATT_DUE_DATE, "\$TODAY")
                    )
                )
            }

            assertThat(found).hasSize(0)
        }
    }

    @Test
    fun `filter by due date time range absolute`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ATT_DUE_DATE, "2021-01-01T09:00:00.0Z/2021-01-02T20:00:00.0Z")
                )
            )
        }

        assertThat(found).hasSize(2)
        assertThat(found.map { it.getLocalId() }).containsAll(listOf("task1", "task2"))
    }

    @Test
    fun `filter by due date time range relatively`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ATT_DUE_DATE, "-P100000D/\$NOW")
                )
            )
        }

        assertThat(found).hasSize(3)
        assertThat(found[0].getLocalId()).isEqualTo("task1", "task2", "task3")
    }

    @Test
    fun `filter task by name`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.eq(ATT_NAME, "task2")
                )
            )
        }

        assertThat(found).hasSize(1)
        assertThat(found[0].getLocalId()).isEqualTo("task2")
    }

    @Test
    fun `filter task by name contains`() {
        val found = AuthContext.runAsFull(HERMIONE_USER) {
            queryTasks(
                Predicates.and(
                    Predicates.eq(ProcTaskSqlQueryBuilder.ATT_ACTOR, ATT_CURRENT_USER_WITH_AUTH),
                    Predicates.contains(ATT_NAME, "task")
                )
            )
        }

        assertThat(found).hasSize(4)
    }

    private fun queryTasks(predicate: Predicate, sortBy: SortBy? = null): List<EntityRef> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withSortBy(sortBy)
                withPage(QueryPage(10_000, 0, null))
            }
        ).getRecords()
    }

    private fun createTaskForFilterAndSort() {
        val taskData = mutableListOf<Map<String, Any>>()
        taskData.add(
            mapOf(
                ATT_NAME to "task1",
                ATT_PRIORITY to 1,
                ATT_DUE_DATE to Instant.parse("2021-01-01T15:00:00.0Z")
            )
        )
        taskData.add(
            mapOf(
                ATT_NAME to "task2",
                ATT_PRIORITY to 2,
                ATT_DUE_DATE to Instant.parse("2021-01-02T15:00:00.0Z")
            )
        )
        taskData.add(
            mapOf(
                ATT_NAME to "task3",
                ATT_PRIORITY to 3,
                ATT_DUE_DATE to Instant.parse("2021-01-03T15:00:00.0Z")
            )
        )
        taskData.add(
            mapOf(
                ATT_NAME to "task4",
                ATT_PRIORITY to 4
            )
        )

        taskData.forEach {
            val task = taskService.newTask(it["name"].toString())
            task.name = it[ATT_NAME] as String

            task.priority = it[ATT_PRIORITY] as Int
            task.assignee = HERMIONE_USER

            it[ATT_DUE_DATE]?.let { dueDate ->
                task.dueDate = Date.from(dueDate as Instant)
            }

            taskService.saveTask(task)
        }
    }

    private fun createTask(
        assignee: String? = null,
        candidateUsers: List<String> = emptyList(),
        candidateGroups: List<String> = emptyList()
    ) {
        val task = taskService.newTask()
        taskService.saveTask(task)

        assignee?.let {
            taskService.setAssignee(task.id, assignee)
        }
        candidateUsers.forEach { taskService.addCandidateUser(task.id, it) }
        candidateGroups.forEach { taskService.addCandidateGroup(task.id, it) }
    }
}
