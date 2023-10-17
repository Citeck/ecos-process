package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.management.JobDefinition
import org.camunda.bpm.engine.management.JobDefinitionQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnJobDefRecords(
    private val managementService: ManagementService
) : AbstractRecordsDao(), RecordsQueryDao, RecordAttsDao {

    companion object {
        const val ID = "bpmn-job-def"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val jobQuery = PredicateUtils.convertToDto(predicate, BpmnJobDefinitionQuery::class.java)
        if (jobQuery.bpmnDefEngine.getLocalId().isBlank() && jobQuery.bpmnProcess.getLocalId().isBlank()) {
            return RecsQueryRes()
        }

        val totalCount = managementService.createJobDefinitionQuery()
            .applyPredicate(predicate)
            .count()

        val jobs = managementService.createJobDefinitionQuery()
            .applyPredicate(predicate)
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


    private fun JobDefinitionQuery.applyPredicate(pred: Predicate): JobDefinitionQuery {
        val bpmnJobDefinitionQuery = PredicateUtils.convertToDto(pred, BpmnJobDefinitionQuery::class.java)

        return this.apply {
            val bpmnDefId = bpmnJobDefinitionQuery.bpmnDefEngine.getLocalId()
            if (bpmnDefId.isNotBlank()) {
                processDefinitionId(bpmnDefId)
            }
        }
    }

    override fun getRecordAtts(recordId: String): Any? {
        return managementService.createJobDefinitionQuery()
            .jobDefinitionId(recordId)
            .singleResult()?.let {
                BpmnJobDefRecord(it)
            }
    }

    private inner class BpmnJobDefRecord(
        private val jobDefinition: JobDefinition,

        val id: String = jobDefinition.id
    ) {

        @AttName("state")
        fun getState(): JobStateRecord {
            return JobStateRecord(
                if (jobDefinition.isSuspended) {
                    BpmnJobState.SUSPENDED
                } else {
                    BpmnJobState.ACTIVE
                }
            )
        }

        @AttName("activityId")
        fun getActivityId(): String {
            return jobDefinition.activityId ?: ""
        }

        @AttName("type")
        fun getType(): String {
            return jobDefinition.jobType ?: ""
        }

        @AttName("configuration")
        fun getConfiguration(): String {
            return jobDefinition.jobConfiguration ?: ""
        }
    }

    data class BpmnJobDefinitionQuery(
        var bpmnDefEngine: EntityRef = EntityRef.EMPTY,
        var bpmnProcess: EntityRef = EntityRef.EMPTY
    )
}
