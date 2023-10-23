package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.runtime.Job
import org.camunda.bpm.engine.runtime.JobQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

@Component
class BpmnJobRecords(
    private val managementService: ManagementService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDao {

    companion object {
        const val ID = "bpmn-job"

        private val log = KotlinLogging.logger {}

        private const val ATT_RETRIES = "retries"
        private const val ATT_DUE_DATE = "dueDate"
        private const val ATT_JOB_ID = "id"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val jobQuery = PredicateUtils.convertToDto(predicate, BpmnJobQuery::class.java)
        if (jobQuery.bpmnProcess.getLocalId().isBlank()) {
            return RecsQueryRes()
        }

        val totalCount = managementService.createJobQuery()
            .applyPredicate(predicate)
            .count()

        val jobs = managementService.createJobQuery()
            .applyPredicate(predicate)
            .applySort(recsQuery)
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems)
            .map {
                EntityRef.create(AppName.EPROC, ID, it.id)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(jobs)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result

    }

    private fun JobQuery.applyPredicate(pred: Predicate): JobQuery {
        val bpmnJobDefinitionQuery = PredicateUtils.convertToDto(pred, BpmnJobQuery::class.java)

        return this.apply {
            val bpmnProcessId = bpmnJobDefinitionQuery.bpmnProcess.getLocalId()
            if (bpmnProcessId.isNotBlank()) {
                processInstanceId(bpmnProcessId)
            }
        }
    }

    private fun JobQuery.applySort(recsQuery: RecordsQuery): JobQuery {
        if (recsQuery.sortBy.isEmpty()) {
            return this.apply {
                orderByJobDuedate()
                desc()
            }
        }

        val sortBy = recsQuery.sortBy[0]
        return this.apply {
            when (sortBy.attribute) {
                ATT_DUE_DATE -> orderByJobDuedate()
                ATT_JOB_ID -> orderByJobId()
                ATT_RETRIES -> orderByJobRetries()
                else -> orderByJobDuedate()
            }

            if (sortBy.ascending) {
                asc()
            } else {
                desc()
            }
        }
    }

    override fun getRecordsAtts(recordIds: List<String>): List<*>? {
        return managementService
            .createJobQuery()
            .jobIds(recordIds.toSet())
            .list()
            .map { BpmnJobRecord(it) }
            .sortByIds(recordIds)
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (record.id.isBlank()) {
            error("Job id is blank: $record")
        }

        when (record.toActionEnum(MutateAction::class.java)) {
            MutateAction.SUSPEND -> {
                log.debug { "Suspend job ${record.id}" }
                managementService.suspendJobById(record.id)
            }

            MutateAction.ACTIVATE -> {
                log.debug { "Activate job ${record.id}" }
                managementService.activateJobById(record.id)
            }

            else -> {
                // do nothing
            }
        }

        if (record.hasAtt(ATT_RETRIES)) {
            val retriesAtt = record.getAtt(ATT_RETRIES)
            if (retriesAtt.isIntegralNumber()) {
                log.debug { "Set retries for job ${record.id} to ${retriesAtt.asInt()}" }
                managementService.setJobRetries(record.id, retriesAtt.asInt())
            }
        }

        return record.id
    }

    private inner class BpmnJobRecord(
        private val job: Job,

        val id: String = job.id
    ) : IdentifiableRecord {

        override fun getIdentificator(): String {
            return id
        }

        @AttName("state")
        fun getState(): JobStateRecord {
            return JobStateRecord(
                if (job.isSuspended) {
                    BpmnJobState.SUSPENDED
                } else {
                    BpmnJobState.ACTIVE
                }
            )
        }

        @AttName(ATT_RETRIES)
        fun getRetries(): Int {
            return job.retries
        }

        @AttName(RecordConstants.ATT_CREATED)
        fun getCreatedDate(): Instant {
            return job.createTime.toInstant()
        }

        @AttName(ATT_DUE_DATE)
        fun getDueDate(): Instant {
            return job.duedate.toInstant()
        }

        @AttName("failedActivityId")
        fun getFailedActivityId(): String {
            return job.failedActivityId ?: ""
        }

        @AttName("jobDefinition")
        fun getJobDefinition(): EntityRef {
            return EntityRef.create(
                AppName.EPROC,
                BpmnJobDefRecords.ID,
                job.jobDefinitionId
            )
        }

        @AttName("exceptionMessage")
        fun getExceptionMessage(): String {
            return job.exceptionMessage ?: ""
        }

        @AttName("stackTrace")
        fun getStackTrace(): String {
            return managementService.getJobExceptionStacktrace(id) ?: ""
        }

    }

    private enum class MutateAction {
        SUSPEND,
        ACTIVATE
    }

    data class BpmnJobQuery(
        var bpmnProcess: EntityRef = EntityRef.EMPTY
    )
}
