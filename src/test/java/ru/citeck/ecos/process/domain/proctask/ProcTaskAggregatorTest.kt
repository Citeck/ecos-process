package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.util.ClockUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.clearTasks
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.AggregateTaskDto
import ru.citeck.ecos.process.domain.proctask.service.aggregate.AlfWorkflowTaskProvider
import ru.citeck.ecos.process.domain.proctask.service.aggregate.ProcTaskAggregator
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.text.SimpleDateFormat

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskAggregatorTest {

    @Autowired
    private lateinit var aggregator: ProcTaskAggregator

    @Autowired
    private lateinit var taskService: TaskService

    @MockBean
    private lateinit var alfWorkflowTaskProvider: AlfWorkflowTaskProvider

    companion object {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS")
    }

    /**
     * Sorted test data
     *
     * alfTask1	12.12.2001 14:00
     * task10	01.12.2001 13:00
     * AlfTask2	01.11.2001 18:00
     * task9	01.10.2001 13:00
     * task8	06.06.2001 13:00
     * task7	02.06.2001 15:00
     * task6	08.04.2001 21:00
     * task5	08.04.2001 19:30
     * task4	05.03.2001 18:00
     * task3	02.01.2001 13:00
     * task2	01.01.2001 13:24
     * task1	01.01.2001 13:00
     * alfTask3	01.01.2000 18:00
     */
    @BeforeEach
    fun setUp() {
        createTestTask("task1", "01/01/2001 13:00:00.000")
        createTestTask("task2", "01/01/2001 13:24:00.000")
        createTestTask("task3", "02/01/2001 13:00:00.000")
        createTestTask("task4", "05/03/2001 18:00:00.000")
        createTestTask("task5", "08/04/2001 19:30:00.000")
        createTestTask("task6", "08/04/2001 21:00:00.000")
        createTestTask("task7", "02/06/2001 15:00:00.000")
        createTestTask("task8", "06/06/2001 13:00:00.000")
        createTestTask("task9", "01/10/2001 13:00:00.000")
        createTestTask("task10", "01/12/2001 13:00:00.000")
    }

    @AfterEach
    fun tearDown() {
        clearTasks()
    }

    private fun createTestTask(id: String, createTime: String) {
        ClockUtil.setCurrentTime(sdf.parse(createTime))

        val task = taskService.newTask(id)
        taskService.saveTask(task)

        taskService.setAssignee(task.id, "harry")
        taskService.addCandidateUser(task.id, "harry")
    }

    @Test
    fun `unlimited aggregate query`() {
        mockAlfQueryTasks(
            listOf(
                AlfTask("alfTask1", "12/12/2001 14:00:00.000"),
                AlfTask("alfTask2", "01/11/2001 18:00:00.000"),
                AlfTask("alfTask3", "01/01/2000 18:00:00.000")
            )
        )

        val query = RecordsQuery.create {
            withMaxItems(10_000)
        }

        val aggregationResult = AuthContext.runAs(user = "harry", authorities = listOf("harry")) {
            aggregator.queryTasks(query)
        }

        assertThat(aggregationResult.getRecords()).isEqualTo(
            listOf(
                "alfTask1".toAggregationRef(),
                "task10".toAggregationRef(),
                "alfTask2".toAggregationRef(),
                "task9".toAggregationRef(),
                "task8".toAggregationRef(),
                "task7".toAggregationRef(),
                "task6".toAggregationRef(),
                "task5".toAggregationRef(),
                "task4".toAggregationRef(),
                "task3".toAggregationRef(),
                "task2".toAggregationRef(),
                "task1".toAggregationRef(),
                "alfTask3".toAggregationRef(),
            )
        )
        assertThat(aggregationResult.getTotalCount()).isEqualTo(13)
        assertThat(aggregationResult.getHasMore()).isEqualTo(false)
    }

    @Test
    fun `aggregate query last page`() {
        mockAlfQueryTasks(
            listOf(
                AlfTask("alfTask1", "12/12/2001 14:00:00.000"),
                AlfTask("alfTask2", "01/11/2001 18:00:00.000"),
                AlfTask("alfTask3", "01/01/2000 18:00:00.000")
            )
        )

        val query = RecordsQuery.create {
            withMaxItems(10)
            withSkipCount(10)
        }

        val aggregationResult = AuthContext.runAs(user = "harry", authorities = listOf("harry")) {
            aggregator.queryTasks(query)
        }

        assertThat(aggregationResult.getRecords()).isEqualTo(
            listOf(
                "task2".toAggregationRef(),
                "task1".toAggregationRef(),
                "alfTask3".toAggregationRef(),
            )
        )
        assertThat(aggregationResult.getTotalCount()).isEqualTo(13)
        assertThat(aggregationResult.getHasMore()).isEqualTo(false)
    }

    @Test
    fun `aggregate query with max items`() {
        mockAlfQueryTasks(
            listOf(
                AlfTask("alfTask1", "12/12/2001 14:00:00.000"),
                AlfTask("alfTask2", "01/11/2001 18:00:00.000")
            )
        )

        val query = RecordsQuery.create {
            withMaxItems(5)
        }

        val aggregationResult = AuthContext.runAs(user = "harry", authorities = listOf("harry")) {
            aggregator.queryTasks(query)
        }

        assertThat(aggregationResult.getRecords()).isEqualTo(
            listOf(
                "alfTask1".toAggregationRef(),
                "task10".toAggregationRef(),
                "alfTask2".toAggregationRef(),
                "task9".toAggregationRef(),
                "task8".toAggregationRef(),
            )
        )
        assertThat(aggregationResult.getTotalCount()).isEqualTo(12)
        assertThat(aggregationResult.getHasMore()).isEqualTo(true)
    }

    @Test
    fun `aggregate query with max items and skip count`() {
        mockAlfQueryTasks(
            listOf(
                AlfTask("alfTask1", "12/12/2001 14:00:00.000"),
                AlfTask("alfTask2", "01/11/2001 18:00:00.000")
            )
        )

        val query = RecordsQuery.create {
            withMaxItems(5)
            withSkipCount(3)
        }

        val aggregationResult = AuthContext.runAs(user = "harry", authorities = listOf("harry")) {
            aggregator.queryTasks(query)
        }

        assertThat(aggregationResult.getRecords()).isEqualTo(
            listOf(
                "task9".toAggregationRef(),
                "task8".toAggregationRef(),
                "task7".toAggregationRef(),
                "task6".toAggregationRef(),
                "task5".toAggregationRef(),
            )
        )
        assertThat(aggregationResult.getTotalCount()).isEqualTo(12)
        assertThat(aggregationResult.getHasMore()).isEqualTo(true)
    }

    @Test
    fun `aggregate query with skip and has more eq false`() {
        mockAlfQueryTasks(
            listOf(
                AlfTask("alfTask1", "12/12/2001 14:00:00.000"),
                AlfTask("alfTask2", "01/11/2001 18:00:00.000")
            )
        )

        val query = RecordsQuery.create {
            withMaxItems(5)
            withSkipCount(9)
        }

        val aggregationResult = AuthContext.runAs(user = "harry", authorities = listOf("harry")) {
            aggregator.queryTasks(query)
        }

        assertThat(aggregationResult.getRecords()).isEqualTo(
            listOf(
                "task3".toAggregationRef(),
                "task2".toAggregationRef(),
                "task1".toAggregationRef(),
            )
        )
        assertThat(aggregationResult.getTotalCount()).isEqualTo(12)
        assertThat(aggregationResult.getHasMore()).isEqualTo(false)
    }

    private fun mockAlfQueryTasks(tasks: List<AlfTask>) {
        val aggregateTasks = tasks.map {
            AggregateTaskDto(
                id = it.id,
                aggregationRef = RecordRef.valueOf("eproc/${ProcTaskRecords.ID}@${it.id}"),
                createTime = sdf.parse(it.createTime)
            )
        }

        val mockedAlfResult = RecsQueryRes(aggregateTasks)
        mockedAlfResult.setTotalCount(aggregateTasks.size.toLong())

        Mockito.`when`(alfWorkflowTaskProvider.queryTasks(any())).thenReturn(
            mockedAlfResult
        )
    }

    data class AlfTask(
        val id: String,
        val createTime: String,
    )
}

fun String.toAggregationRef(): RecordRef {
    return RecordRef.create("eproc", ProcTaskRecords.ID, this)
}
