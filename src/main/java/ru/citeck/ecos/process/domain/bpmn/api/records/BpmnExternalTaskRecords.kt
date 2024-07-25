package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.ExternalTaskService
import org.camunda.bpm.engine.externaltask.ExternalTask
import org.camunda.bpm.engine.externaltask.ExternalTaskQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.service.BpmnPermissionResolver
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
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

@Component
class BpmnExternalTaskRecords(
    private val externalTaskService: ExternalTaskService,
    private val bpmnPermissionResolver: BpmnPermissionResolver
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDao {

    companion object {
        const val ID = "bpmn-external-task"
        const val ATT_RETRIES = "retries"

        private val log = KotlinLogging.logger {}

        private const val ATT_ID = "id"
        private const val ATT_PRIORITY = "priority"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, BpmnExternalTaskQuery::class.java)
        val processId = query.bpmnProcess.getLocalId()
        check(processId.isNotBlank()) {
            "Process id must be specified"
        }

        if (!bpmnPermissionResolver.isAllowForProcessInstanceId(BpmnPermission.PROC_INSTANCE_READ, processId)) {
            return RecsQueryRes()
        }

        val totalCount = externalTaskService.createExternalTaskQuery()
            .applyPredicate(predicate)
            .count()

        val jobs = externalTaskService.createExternalTaskQuery()
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

    private fun ExternalTaskQuery.applyPredicate(pred: Predicate): ExternalTaskQuery {
        val externalTaskQuery = PredicateUtils.convertToDto(pred, BpmnExternalTaskQuery::class.java)

        return this.apply {
            val bpmnProcessId = externalTaskQuery.bpmnProcess.getLocalId()
            if (bpmnProcessId.isNotBlank()) {
                processInstanceId(bpmnProcessId)
            }

            if (externalTaskQuery.id.isNotBlank()) {
                externalTaskId(externalTaskQuery.id)
            }

            if (externalTaskQuery.workerId.isNotBlank()) {
                workerId(externalTaskQuery.workerId)
            }

            if (externalTaskQuery.topicName.isNotBlank()) {
                topicName(externalTaskQuery.topicName)
            }

            if (externalTaskQuery.activityId.isNotBlank()) {
                activityId(externalTaskQuery.activityId)
            }
        }
    }

    private fun ExternalTaskQuery.applySort(recsQuery: RecordsQuery): ExternalTaskQuery {
        if (recsQuery.sortBy.isEmpty()) {
            return this.apply {
                orderByPriority()
                desc()
            }
        }

        val sortBy = recsQuery.sortBy[0]
        return this.apply {
            when (sortBy.attribute) {
                ATT_ID -> orderById()
                ATT_PRIORITY -> orderByPriority()
                else -> orderByPriority()
            }

            if (sortBy.ascending) {
                asc()
            } else {
                desc()
            }
        }
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (record.id.isBlank()) {
            error("External task id is blank: $record")
        }

        val externalTask = externalTaskService.createExternalTaskQuery()
            .externalTaskId(record.id)
            .singleResult()
        val processInstance = externalTask.processInstanceId
        if (!bpmnPermissionResolver.isAllowForProcessInstanceId(BpmnPermission.PROC_INSTANCE_EDIT, processInstance)) {
            error("User has no permissions to edit process instance: $processInstance")
        }

        if (record.hasAtt(ATT_RETRIES)) {
            val retriesAtt = record.getAtt(ATT_RETRIES)
            if (retriesAtt.isIntegralNumber()) {
                log.debug { "Set retries for bpmn external task ${record.id} to ${retriesAtt.asInt()}" }
                externalTaskService.setRetries(record.id, retriesAtt.asInt())
            }
        }

        return record.id
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any?> {
        return externalTaskService.createExternalTaskQuery()
            .externalTaskIdIn(recordIds.toSet())
            .list()
            .checkPermissionAndReplaceToEmptyRecord()
            .map {
                when (it) {
                    is ExternalTask -> ExternalTaskRecord(it)
                    is EmptyIdentifiableRecord -> it
                    else -> error("Unknown record type: $it")
                }
            }
            .sortByIds(recordIds)
    }

    private fun MutableList<ExternalTask>.checkPermissionAndReplaceToEmptyRecord(): MutableList<Any> {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return this.toMutableList()
        }

        return map {
            val processInstanceId = it.processInstanceId
            if (bpmnPermissionResolver.isAllowForProcessInstanceId(
                    BpmnPermission.PROC_INSTANCE_READ,
                    processInstanceId
                )
            ) {
                it
            } else {
                EmptyIdentifiableRecord(it.id)
            }
        }.toMutableList()
    }

    private inner class ExternalTaskRecord(
        private val externalTask: ExternalTask,

        val id: String = externalTask.id
    ) : IdentifiableRecord {

        @AttName("stackTrace")
        fun getStackTrace(): String {
            return externalTaskService.getExternalTaskErrorDetails(externalTask.id) ?: ""
        }

        @AttName("activityId")
        fun getActivityId(): String {
            return externalTask.activityId ?: ""
        }

        @AttName("retries")
        fun getRetries(): Int {
            return externalTask.retries ?: 0
        }

        @AttName("workerId")
        fun getWorkerId(): String {
            return externalTask.workerId ?: ""
        }

        @AttName("lockExpirationTime")
        fun getLockExpirationTime(): Instant? {
            return externalTask.lockExpirationTime?.toInstant()
        }

        @AttName("topic")
        fun getTopic(): String {
            return externalTask.topicName ?: ""
        }

        @AttName("priority")
        fun getPriority(): Long {
            return externalTask.priority
        }

        override fun getIdentificator(): String {
            return id
        }
    }

    data class BpmnExternalTaskQuery(
        var bpmnProcess: EntityRef = EntityRef.EMPTY,
        var id: String = "",
        var workerId: String = "",
        var topicName: String = "",
        var activityId: String = ""
    )
}
