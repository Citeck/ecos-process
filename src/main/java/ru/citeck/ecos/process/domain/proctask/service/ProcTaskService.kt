package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

interface ProcTaskService {

    fun getTasksByProcess(processId: String): List<ProcTaskDto>

    fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto>

    fun getTasksByDocument(document: String): List<ProcTaskDto>

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto>

    fun getTaskById(taskId: String): ProcTaskDto?

    fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?>

    fun completeTask(taskId: String, outcome: Outcome, variables: Map<String, Any?>)

    fun getVariables(taskId: String): Map<String, Any?>

    fun claimTask(taskId: String, userId: String)

    fun unclaimTask(taskId: String)

    fun setAssignee(taskId: String, userId: String)
}
