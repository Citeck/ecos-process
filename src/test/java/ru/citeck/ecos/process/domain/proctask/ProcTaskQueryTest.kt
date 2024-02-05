package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.clearTasks
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.service.ATT_CURRENT_USER_WITH_AUTH
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskSqlQueryBuilder
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

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
                assignee = HARRY_USER, candidateUsers = listOf(HARRY_USER),
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
                assignee = RON_USER, candidateUsers = listOf(RON_USER),
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
                assignee = VOLDEMORT_USER, candidateUsers = listOf(VOLDEMORT_USER),
                candidateGroups = listOf(DEATH_EATERS_GROUP)
            )
        }

        for (i in 1..5) {
            createTask(candidateGroups = listOf(DEATH_EATERS_GROUP))
        }
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

    private fun queryTasks(predicate: Predicate): List<EntityRef> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withPage(QueryPage(10_000, 0, null))
            }
        ).getRecords()
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
