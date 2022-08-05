package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.apps.EcosWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName

@Component
class WorkflowTaskRecordsProxy(
    private val procTaskService: ProcTaskService,
    private val webAppsApi: EcosWebAppsApi
) : RecordsDaoProxy(
    id = "wftask",
    targetId = "alfresco/wftask"
) {

    companion object {
        private const val CURRENT_USER_FLAG = "\$CURRENT"
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*> {
        val query = recsQuery.getQuery(TaskQuery::class.java)
        val document = RecordRef.valueOf(query.document)

        if (documentIsProcess(document)) {
            return queryTasksForAnyProcessEngine(recsQuery, document)
        }

        val documentTasksFromAlf = queryFromAlf(recsQuery)
        val documentTasksFromEProc = queryTasksForDocumentFromEcosProcess(query)

        val result = RecsQueryRes<Any>()
        result.setRecords(documentTasksFromAlf.getRecords() + documentTasksFromEProc.getRecords())
        result.setTotalCount(documentTasksFromAlf.getTotalCount() + documentTasksFromEProc.getTotalCount())

        return result
    }

    private fun queryTasksForDocumentFromEcosProcess(query: TaskQuery): RecsQueryRes<*> {
        val result = RecsQueryRes<RecordRef>()
        if (query.document.isBlank()) return result

        val taskRefs = if (query.actor == CURRENT_USER_FLAG) {
            procTaskService.getTasksByDocumentForCurrentUser(query.document)
        } else {
            procTaskService.getTasksByDocument(query.document)
        }.map {
            RecordRef.create("eproc", ProcTaskRecords.ID, it.id)
        }

        result.setRecords(taskRefs)
        result.setTotalCount(taskRefs.size.toLong())

        return result
    }

    private fun queryTasksForAnyProcessEngine(recsQuery: RecordsQuery, processRef: RecordRef): RecsQueryRes<*> {
        return when {
            isAlfProcess(processRef) -> queryFromAlf(recsQuery)
            isEcosProcProcess(processRef) -> queryTasksForEcosProcess(recsQuery, processRef)
            else -> throw IllegalStateException("Unsupported state. Query: $recsQuery")
        }
    }

    private fun queryFromAlf(recsQuery: RecordsQuery): RecsQueryRes<*> {
        return if (webAppsApi.isAppAvailable(AppName.ALFRESCO)) {
            super.queryRecords(recsQuery)
        } else {
            null
        } ?: RecsQueryRes<Any>()
    }

    private fun queryTasksForEcosProcess(recordsQuery: RecordsQuery, processRef: RecordRef): RecsQueryRes<*> {
        val result = RecsQueryRes<RecordRef>()
        val taskQuery = recordsQuery.getQuery(TaskQuery::class.java)

        val taskRefs = if (taskQuery.actor == CURRENT_USER_FLAG) {
            procTaskService.getTasksByProcessForCurrentUser(processRef.id)
        } else {
            procTaskService.getTasksByProcess(processRef.id)
        }.map {
            RecordRef.create("eproc", ProcTaskRecords.ID, it.id)
        }

        result.setRecords(taskRefs)
        result.setTotalCount(taskRefs.size.toLong())

        return result
    }

    data class TaskQuery(
        var active: Boolean,
        var document: String = "",
        var actor: String = ""
    )
}

private fun documentIsProcess(document: RecordRef): Boolean {
    return isAlfProcess(document) || isEcosProcProcess(document)
}

private fun isAlfProcess(ref: RecordRef): Boolean {
    return ref.id.contains("$")
}

private fun isEcosProcProcess(ref: RecordRef): Boolean {
    return ref.sourceId == BpmnProcRecords.ID
}
