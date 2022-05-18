package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

@Component
class WorkflowTaskRecordsProxy(
    private val procTaskService: ProcTaskService
) : RecordsDaoProxy(
    id = "wftask",
    targetId = "alfresco/wftask"
) {

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        val query = recsQuery.getQuery(TaskQuery::class.java)
        val document = RecordRef.Companion.valueOf(query.document)

        if (document.id.contains("$")) {
            return super.queryRecords(recsQuery)
        }

        if (document.sourceId == BpmnProcRecords.ID) {
            val result = RecsQueryRes<RecordRef>()

            val taskRefs = procTaskService.getTasksByProcess(document.id).map {
                RecordRef.create("eproc", ProcTaskRecords.ID, it.id)
            }

            result.setRecords(taskRefs)

            return result
        }

        return super.queryRecords(recsQuery)
    }

    data class TaskQuery(
        var active: Boolean,
        var document: String = ""
    )
}
