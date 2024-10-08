package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.predicate.model.AndPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.apps.EcosRemoteWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

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
        val document = EntityRef.valueOf(query.document)
        val sortBy = recsQuery.sortBy
        val page = recsQuery.page

        if (documentIsProcess(document)) {
            return queryTasksForAnyProcessEngine(recsQuery, document)
        }

        val documentTasksFromAlf = queryFromAlf(recsQuery)

        /*
        Do not request Camunda tasks with Alfresco enabled due to complexities with filtering, sorting, and merging
        search results. Expect that requests without a "document" with Alfresco enabled will only come from the mobile application
         */
        val documentTasksFromEProc: RecsQueryRes<*> = if (webAppsApi.isAppAvailable(AppName.ALFRESCO) && document.isEmpty()) {
            RecsQueryRes<Any>()
        } else {
            queryTasksForDocumentFromEcosProcess(query, sortBy, page)
        }
        val result = RecsQueryRes<Any>()
        result.setRecords(documentTasksFromAlf.getRecords() + documentTasksFromEProc.getRecords())
        result.setTotalCount(documentTasksFromAlf.getTotalCount() + documentTasksFromEProc.getTotalCount())
        result.setHasMore(documentTasksFromAlf.getHasMore() || documentTasksFromEProc.getHasMore())

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
                RecordAtts(EntityRef.create(AppName.ALFRESCO, "wftask", it.id), it.attributes)
            } else {
                RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, it.id), it.attributes)
            }
        }

        return recordsService.mutate(toMutate).map { it.getLocalId() }
    }

    private fun queryTasksForDocumentFromEcosProcess(query: TaskQuery, sortBy: List<SortBy>, page: QueryPage): RecsQueryRes<*> {
        val result = RecsQueryRes<EntityRef>()
        if (!query.active) {
            return result
        }
        val conditions = mutableListOf<Predicate>()

        if (!query.document.isBlank()) {
            conditions.add(Predicates.eq("document", query.document))
        }
        if (query.actor.isNotEmpty()) {
            conditions.add(Predicates.eq("actor", query.actor))
        }

        val tasksQueryRes = procTaskService.findTasks(AndPredicate.of(conditions), sortBy, page)
        result.setRecords(
            tasksQueryRes.entities.map {
                EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, it)
            }
        )
        result.setTotalCount(tasksQueryRes.totalCount)
        return result
    }

    private fun queryTasksForAnyProcessEngine(recsQuery: RecordsQuery, processRef: EntityRef): RecsQueryRes<*> {
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

    private fun queryTasksForEcosProcess(recordsQuery: RecordsQuery, processRef: EntityRef): RecsQueryRes<*> {
        val result = RecsQueryRes<EntityRef>()
        val taskQuery = recordsQuery.getQuery(TaskQuery::class.java)

        val taskRefs = if (taskQuery.actor == CURRENT_USER_FLAG) {
            procTaskService.getTasksByProcessForCurrentUser(processRef.getLocalId())
        } else {
            procTaskService.getTasksByProcess(processRef.getLocalId())
        }.map {
            EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, it.id)
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

private fun documentIsProcess(document: EntityRef): Boolean {
    return isAlfProcess(document) || isEcosProcProcess(document)
}

private fun isAlfProcess(ref: EntityRef): Boolean {
    return ref.getLocalId().containsAlfDelimiter()
}

private fun String.containsAlfDelimiter(): Boolean {
    return this.contains("$")
}

private fun isEcosProcProcess(ref: EntityRef): Boolean {
    return ref.getSourceId() == BpmnProcessRecords.ID
}
