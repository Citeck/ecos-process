package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

const val TASK_COMPLETED_BY = "completedBy"

interface ProcTaskService {

    fun getTasksByProcess(processId: String): List<ProcTaskDto>

    fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto>

    fun getTasksByDocument(document: String): List<ProcTaskDto>

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto>

    fun getTaskById(taskId: String): ProcTaskDto?

    fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?>

    fun completeTask(taskId: String, variables: Map<String, Any?>)
}
