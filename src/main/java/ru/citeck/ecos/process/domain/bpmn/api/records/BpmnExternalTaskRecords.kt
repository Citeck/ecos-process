package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.ExternalTaskService
import org.camunda.bpm.engine.externaltask.ExternalTask
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao

@Component
class BpmnExternalTaskRecords(
    private val externalTaskService: ExternalTaskService
) : AbstractRecordsDao(), RecordsAttsDao, RecordMutateDao {

    companion object {
        const val ID = "bpmn-external-task"

        private const val ATT_RETRIES = "retries"
    }

    override fun getId() = ID

    override fun mutate(record: LocalRecordAtts): String {
        if (record.id.isBlank()) {
            error("External task id is blank: $record")
        }

        if (record.hasAtt(ATT_RETRIES)) {
            val retriesAtt = record.getAtt(ATT_RETRIES)
            if (retriesAtt.isIntegralNumber()) {
                externalTaskService.setRetries(record.id, retriesAtt.asInt())
            }
        }

        return record.id
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        return externalTaskService.createExternalTaskQuery()
            .externalTaskIdIn(recordIds.toSet())
            .list()
            .map { ExternalTaskRecord(it) }
            .sortByIds(recordIds)
    }

    private inner class ExternalTaskRecord(
        private val externalTask: ExternalTask,

        val id: String = externalTask.id
    ) : IdentifiableRecord {

        @AttName("stackTrace")
        fun getStackTrace(): String {
            return externalTaskService.getExternalTaskErrorDetails(externalTask.id) ?: ""
        }

        override fun getIdentificator(): String {
            return id
        }

    }
}
