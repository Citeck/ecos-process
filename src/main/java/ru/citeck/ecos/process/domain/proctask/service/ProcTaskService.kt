package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy

interface ProcTaskService {

    fun getCurrentUserTasksIds(predicate: Predicate, page: QueryPage, sortBy: List<SortBy>): DbFindRes<String>

    fun getTasksByProcess(processId: String): List<ProcTaskDto>

    fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto>

    fun getTasksByDocument(document: String): List<ProcTaskDto>

    fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto>

    fun getTaskById(taskId: String): ProcTaskDto?

    fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?>

    fun completeTask(taskId: String, outcome: Outcome, variables: Map<String, Any?>)

    fun getVariables(taskId: String): Map<String, Any?>
}
