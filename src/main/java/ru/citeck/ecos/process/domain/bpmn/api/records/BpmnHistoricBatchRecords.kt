package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.batch.history.HistoricBatch
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import java.time.Instant

@Component
class BpmnHistoricBatchRecords(
    private val historyService: HistoryService
) : AbstractRecordsDao(),
    RecordAttsDao {

    companion object {
        const val ID = "bpmn-historic-batch"
    }

    override fun getId() = ID

    override fun getRecordAtts(recordId: String): Any? {
        val batch = historyService.createHistoricBatchQuery()
            .batchId(recordId)
            .singleResult() ?: return null

        return BpmnBatchRecord(batch)
    }

    private inner class BpmnBatchRecord(
        private val batchDto: HistoricBatch,

        val id: String = batchDto.id
    ) {

        @AttName("type")
        fun getType(): String {
            return batchDto.type ?: ""
        }

        @AttName("totalJobs")
        fun getTotalJobs(): Int {
            return batchDto.totalJobs
        }

        @AttName("batchJobsPerSeed")
        fun getBatchJobsPerSeed(): Int {
            return batchDto.batchJobsPerSeed
        }

        @AttName("invocationsPerBatchJob")
        fun getInvocationsPerBatchJob(): Int {
            return batchDto.invocationsPerBatchJob
        }

        @AttName("seedJobDefinitionId")
        fun getSeedJobDefinitionId(): String {
            return batchDto.seedJobDefinitionId ?: ""
        }

        @AttName("monitorJobDefinitionId")
        fun getMonitorJobDefinitionId(): String {
            return batchDto.monitorJobDefinitionId ?: ""
        }

        @AttName("batchJobDefinitionId")
        fun getBatchJobDefinitionId(): String {
            return batchDto.batchJobDefinitionId ?: ""
        }

        @AttName("tenantId")
        fun getTenantId(): String {
            return batchDto.tenantId ?: ""
        }

        @AttName("createUserId")
        fun getCreateUserId(): String {
            return batchDto.createUserId ?: ""
        }

        @AttName("startTime")
        fun getStartTime(): Instant? {
            return batchDto.startTime?.toInstant()
        }

        @AttName("endTime")
        fun getEndTime(): Instant? {
            return batchDto.endTime?.toInstant()
        }

        @AttName("removalTime")
        fun getRemovalTime(): Instant? {
            return batchDto.removalTime?.toInstant()
        }
    }
}
