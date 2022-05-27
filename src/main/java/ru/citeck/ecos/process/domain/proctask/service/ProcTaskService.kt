package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

interface ProcTaskService {

    fun getTasksByProcess(processId: String): List<ProcTaskDto>

    fun getTasksByDocument(document: String): List<ProcTaskDto>

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto>

    fun getTaskById(taskId: String): ProcTaskDto?

    fun submitTaskForm(taskId: String, variables: Map<String, Any?>)
}
