package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy

interface ProcTaskService {

    fun findTasks(predicate: Predicate): DbFindRes<String>

    fun findTasks(predicate: Predicate, sortBy: List<SortBy>, page: QueryPage): DbFindRes<String>

    fun getTasksByProcess(processInstanceId: String): List<ProcTaskDto>

    fun getTasksByProcessForCurrentUser(processInstanceId: String): List<ProcTaskDto>

    fun getTasksByDocument(document: String): List<ProcTaskDto>

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto>

    fun getTaskIdsByDocumentType(documentType: String): List<String>

    fun getTaskIdsByDocumentType(documentType: String, skipCount: Int, maxCount: Int): List<String>

    fun getTaskById(taskId: String): ProcTaskDto?

    fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?>

    fun completeTask(completeData: CompleteTaskData)

    fun getVariables(taskId: String): Map<String, Any?>

    fun getVariable(taskId: String, variableName: String): Any?

    fun getVariablesLocal(taskId: String): Map<String, Any?>

    fun setVariablesLocal(taskId: String, variables: Map<String, Any?>)

    fun getVariableLocal(taskId: String, variableName: String): Any?

    fun getVariablesLocal(taskId: String, variableNames: List<String>): Map<String, Any?>

    fun claimTask(taskId: String, userId: String)

    fun unclaimTask(taskId: String)

    fun setAssignee(taskId: String, userId: String)
}
