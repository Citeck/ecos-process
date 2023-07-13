package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.AndPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.apps.EcosRemoteWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName

const val CURRENT_USER_FLAG = "\$CURRENT"

@Component
class WorkflowTaskRecordsProxy(
    private val procTaskService: ProcTaskService,
    private val procTaskRecords: ProcTaskRecords,
    private val webAppsApi: EcosRemoteWebAppsApi
) : RecordsDaoProxy(
    id = "wftask",
    targetId = "alfresco/wftask"
) {

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

    override fun getRecordsAtts(recordIds: List<String>): List<*>? {
        if (recordIds.isEmpty()) {
            return emptyList<RecordAtts>()
        }

        val firstRecord = recordIds.first()

        // support only one source in one request
        return if (firstRecord.containsAlfDelimiter()) {
            super.getRecordsAtts(recordIds)
        } else {
            procTaskRecords.getRecordsAtts(recordIds)
        }
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {
        if (records.isEmpty()) {
            return emptyList()
        }

        val toMutate = records.map {
            if (it.id.containsAlfDelimiter()) {
                RecordAtts(RecordRef.create(AppName.ALFRESCO, "wftask", it.id), it.attributes)
            } else {
                RecordAtts(RecordRef.create(AppName.EPROC, ProcTaskRecords.ID, it.id), it.attributes)
            }
        }

        return recordsService.mutate(toMutate).map { it.id }
    }

    private fun queryTasksForDocumentFromEcosProcess(query: TaskQuery): RecsQueryRes<*> {
        val result = RecsQueryRes<RecordRef>()
        if (query.document.isBlank() || !query.active) {
            return result
        }
        val conditions = mutableListOf<Predicate>(
            Predicates.eq("document", query.document)
        )
        if (query.actor.isNotEmpty()) {
            conditions.add(Predicates.eq("actor", query.actor))
        }

        val tasksQueryRes = procTaskService.findTasks(AndPredicate.of(conditions))
        result.setRecords(
            tasksQueryRes.entities.map {
                RecordRef.create(AppName.EPROC, ProcTaskRecords.ID, it)
            }
        )
        result.setTotalCount(tasksQueryRes.totalCount)
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
            RecordRef.create(AppName.EPROC, ProcTaskRecords.ID, it.id)
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
    return ref.id.containsAlfDelimiter()
}

private fun String.containsAlfDelimiter(): Boolean {
    return this.contains("$")
}

private fun isEcosProcProcess(ref: RecordRef): Boolean {
    return ref.sourceId == BpmnProcessRecords.ID
}
