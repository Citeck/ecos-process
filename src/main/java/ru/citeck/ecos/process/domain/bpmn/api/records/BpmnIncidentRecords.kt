package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.runtime.Incident
import org.camunda.bpm.engine.runtime.IncidentQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import kotlin.system.measureTimeMillis

@Component
class BpmnIncidentRecords(
    private val camundaRuntimeService: RuntimeService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "bpmn-incident"

        private const val ATT_PROCESS_INSTANCE = "processInstance"
        private const val ATT_ACTIVITY_ID = "activityId"
        private const val ATT_INCIDENT_TYPE = "incidentType"
        private const val ATT_NOTE = "note"
    }

    override fun getId() = ID

    override fun mutate(record: LocalRecordAtts): String {
        if (record.id.isBlank()) {
            error("Incident id is blank: $record")
        }

        if (record.hasAtt(ATT_NOTE)) {
            val note = record.getAtt(ATT_NOTE).asText()
            camundaRuntimeService.setAnnotationForIncidentById(record.id, note)
        }

        return record.id
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, BpmnIncidentQuery::class.java)
        if (query.bpmnDefEngine.getLocalId().isBlank() && query.bpmnProcess.getLocalId().isBlank()) {
            return RecsQueryRes()
        }

        val totalCount: Long
        val totalCountTime = measureTimeMillis {
            totalCount = camundaRuntimeService.createIncidentQuery()
                .applyPredicate(predicate)
                .count()
        }
        if (totalCount == 0L) {
            return RecsQueryRes()
        }

        val incidents: List<EntityRef>
        val incidentsTime = measureTimeMillis {
            incidents = camundaRuntimeService.createIncidentQuery()
                .applyPredicate(predicate)
                .applySort(recsQuery)
                .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                    EntityRef.create(AppName.EPROC, ID, it.id)
                }
        }

        log.debug { "$ID total count time: $totalCountTime, incidents time: $incidentsTime" }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(incidents)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    private fun IncidentQuery.applyPredicate(pred: Predicate): IncidentQuery {
        val bpmnIncidentQuery = PredicateUtils.convertToDto(pred, BpmnIncidentQuery::class.java)

        return this.apply {
            if (bpmnIncidentQuery.message.isNotBlank()) {
                incidentMessageLike("%${bpmnIncidentQuery.message}%")
            }

            val bpmnDefId = bpmnIncidentQuery.bpmnDefEngine.getLocalId()
            val bpmnProcId = bpmnIncidentQuery.bpmnProcess.getLocalId()
            if (bpmnDefId.isNotBlank()) {
                processDefinitionId(bpmnDefId)
            } else if (bpmnProcId.isNotBlank()) {
                processInstanceId(bpmnProcId)
            }
        }
    }

    private fun IncidentQuery.applySort(recsQuery: RecordsQuery): IncidentQuery {
        if (recsQuery.sortBy.isEmpty()) {
            return this.apply {
                orderByIncidentTimestamp()
                desc()
            }
        }

        val sortBy = recsQuery.sortBy[0]
        return this.apply {
            when (sortBy.attribute) {
                RecordConstants.ATT_CREATED -> orderByIncidentTimestamp()
                ATT_ACTIVITY_ID -> orderByActivityId()
                ATT_INCIDENT_TYPE -> orderByIncidentType()
                ATT_PROCESS_INSTANCE -> orderByProcessInstanceId()
                else -> orderByIncidentTimestamp()
            }

            if (sortBy.ascending) {
                asc()
            } else {
                desc()
            }
        }
    }

    override fun getRecordAtts(recordId: String): Any? {
        val incident = camundaRuntimeService.createIncidentQuery()
            .incidentId(recordId)
            .singleResult()

        return incident?.let { IncidentRecord(it) }
    }

    private inner class IncidentRecord(
        private val incident: Incident,

        val id: String = incident.id
    ) {

        @AttName(RecordConstants.ATT_CREATED)
        fun getCreated(): Instant {
            return incident.incidentTimestamp.toInstant()
        }

        @AttName("message")
        fun getMessage(): String {
            return incident.incidentMessage ?: ""
        }

        @AttName(ATT_PROCESS_INSTANCE)
        fun getProcessInstance(): EntityRef {
            return EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, incident.processInstanceId)
        }

        @AttName(".disp")
        fun getDisp(): String {
            return id
        }

        @AttName(ATT_ACTIVITY_ID)
        fun getActivityId(): String {
            return incident.activityId ?: ""
        }

        @AttName("failedActivityId")
        fun getFailedActivityId(): String {
            return incident.failedActivityId ?: ""
        }

        @AttName(ATT_INCIDENT_TYPE)
        fun getIncidentType(): IncidentTypeRecord? {
            val type = BpmnIncidentType.getById(incident.incidentType) ?: return null
            return IncidentTypeRecord(type)
        }

        @AttName("causeRef")
        fun getCauseRef(): EntityRef {
            val type = BpmnIncidentType.getById(incident.incidentType)

            return when (type) {
                BpmnIncidentType.FAILED_EXTERNAL_TASK -> {
                    EntityRef.create(
                        AppName.EPROC,
                        BpmnExternalTaskRecords.ID,
                        incident.configuration
                    )
                }

                BpmnIncidentType.FAILED_JOB -> {
                    EntityRef.create(
                        AppName.EPROC,
                        BpmnJobRecords.ID,
                        incident.configuration
                    )
                }

                else -> EntityRef.EMPTY
            }
        }

        @AttName("note")
        fun getNote(): String {
            return incident.annotation ?: ""
        }

    }

    private class IncidentTypeRecord(
        private val type: BpmnIncidentType
    ) {

        @AttName(".disp")
        fun getDisp(): MLText {
            return type.disp
        }

        @AttName("type")
        fun getType(): String {
            return type.id
        }
    }
}

data class BpmnIncidentQuery(
    var bpmnDefEngine: EntityRef = EntityRef.EMPTY,
    var bpmnProcess: EntityRef = EntityRef.EMPTY,
    var message: String = ""
)

enum class BpmnIncidentType(
    val id: String,
    val disp: MLText
) {
    FAILED_JOB(
        "failedJob", MLText(
            I18nContext.ENGLISH to "Job failed",
            I18nContext.RUSSIAN to "Ошибка фоновой задачи"
        )
    ),
    FAILED_EXTERNAL_TASK(
        "failedExternalTask", MLText(
            I18nContext.ENGLISH to "External task failed",
            I18nContext.RUSSIAN to "Ошибка внешней задачи"
        )
    );

    companion object {
        fun getById(id: String): BpmnIncidentType? {
            return BpmnIncidentType.values().find { it.id == id }
        }
    }
}

