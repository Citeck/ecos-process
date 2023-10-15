package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.management.JobDefinition
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
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
        val jobQuery = PredicateUtils.convertToDto(predicate, BpmnJobQuery::class.java)
        if (jobQuery.bpmnDefEngine.isEmpty()) {
            return RecsQueryRes()
        }

        val jobId = jobQuery.bpmnDefEngine.getLocalId()

        val totalCount = managementService.createJobDefinitionQuery()
            .processDefinitionId(jobId)
            .count()

        val jobs = managementService.createJobDefinitionQuery()
            .processDefinitionId(jobId)
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.id)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(jobs)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordAtts(recordId: String): Any? {
        return managementService.createJobDefinitionQuery()
            .jobDefinitionId(recordId)
            .singleResult()?.let {
                BpmnJobRecord(it)
            }
    }

    private inner class BpmnJobRecord(
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

    data class BpmnJobQuery(
        var bpmnDefEngine: EntityRef = EntityRef.EMPTY
    )

    private class JobStateRecord(
        private val state: BpmnJobState
    ) {

        @AttName("suspended")
        fun getIsSuspended(): Boolean {
            return state == BpmnJobState.SUSPENDED
        }

        @AttName(".disp")
        fun getDisp(): MLText {
            return state.disp
        }
    }
}

enum class BpmnJobState(
    val disp: MLText
) {
    ACTIVE(
        MLText(
            I18nContext.ENGLISH to "Active",
            I18nContext.RUSSIAN to "Активно"
        )
    ),
    SUSPENDED(
        MLText(
            I18nContext.ENGLISH to "Suspended",
            I18nContext.RUSSIAN to "Приостановлено"
        )
    );
}
