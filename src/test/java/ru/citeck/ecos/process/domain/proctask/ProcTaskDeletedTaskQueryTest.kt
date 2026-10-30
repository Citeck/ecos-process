package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.task.Task
import org.camunda.bpm.engine.task.TaskQuery
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskServiceImpl
import java.time.Instant

class ProcTaskDeletedTaskQueryTest {

    private val taskService: TaskService = mock()
    private val taskConverter: TaskConverter = mock()
    private val service = ProcTaskServiceImpl(
        authoritiesApi = mock(),
        camundaTaskService = taskService,
        camundaTaskFormService = mock(),
        cacheableTaskConverter = mock(),
        delegationService = mock(),
        commentsService = mock(),
        recordsService = mock(),
        procTaskAttsSyncService = mock(),
        taskConverter = taskConverter,
        taskActorsUtils = mock(),
        processEngineConfiguration = mock(),
        bpmnElementConverter = mock(),
        bpmnEventEmitter = mock()
    )

    @Test
    fun `document query excludes deleted engine tasks before cached DTO conversion`() {
        val deleted = TaskEntity().apply {
            id = "completing-task"
            isDeleted = true
        }
        val active = TaskEntity().apply { id = "active-task" }
        val activeDto = ProcTaskDto(id = active.id, created = Instant.EPOCH)
        prepareQuery(listOf(deleted, active))
        // A cached DTO can still describe the task as active. It must never be requested.
        whenever(taskConverter.toProcTask(deleted)).thenReturn(ProcTaskDto(id = deleted.id, created = Instant.EPOCH, isDeleted = false))
        whenever(taskConverter.toProcTask(active)).thenReturn(activeDto)

        assertThat(service.getTasksByDocument(DOCUMENT)).containsExactly(activeDto)

        verify(taskConverter, never()).toProcTask(deleted)
        verify(taskConverter).toProcTask(active)
    }

    @Test
    fun `document query with only deleted tasks returns no synchronization targets`() {
        val deleted = TaskEntity().apply {
            id = "completing-task"
            isDeleted = true
        }
        prepareQuery(listOf(deleted))
        whenever(taskConverter.toProcTask(deleted)).thenReturn(ProcTaskDto(id = deleted.id, created = Instant.EPOCH))

        assertThat(service.getTasksByDocument(DOCUMENT)).isEmpty()

        verify(taskConverter, never()).toProcTask(deleted)
    }

    @Test
    fun `document query retains task implementations without engine deletion state`() {
        val task: Task = mock()
        val dto = ProcTaskDto(id = "task", created = Instant.EPOCH)
        prepareQuery(listOf(task))
        whenever(taskConverter.toProcTask(task)).thenReturn(dto)

        assertThat(service.getTasksByDocument(DOCUMENT)).containsExactly(dto)
    }

    private fun prepareQuery(tasks: List<Task>) {
        val query: TaskQuery = mock()
        whenever(taskService.createTaskQuery()).thenReturn(query)
        whenever(query.processVariableValueEquals(BPMN_DOCUMENT_REF, DOCUMENT)).thenReturn(query)
        whenever(query.initializeFormKeys()).thenReturn(query)
        whenever(query.list()).thenReturn(tasks)
    }

    companion object {
        private const val DOCUMENT = "alfresco/skif-incident@deleted-task-regression"
    }
}
