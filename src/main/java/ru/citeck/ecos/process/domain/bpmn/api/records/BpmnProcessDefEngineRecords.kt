package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.service.ActivityStatistics
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessStatistics
import ru.citeck.ecos.process.domain.bpmn.service.IncidentStatistics
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

@Component
class BpmnProcessDefEngineRecords(
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService,
    private val managementService: ManagementService,
    private val camundaRuntimeService: RuntimeService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "bpmn-def-engine"

        private const val KEY_ATT = "key"
        private const val VERSION_ATT = "version"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)

        val totalCount: Long
        val totalCountTime = measureTimeMillis {
            totalCount = camundaRepositoryService
                .createProcessDefinitionQuery()
                .applyPredicate(predicate)
                .count()
        }
        if (totalCount == 0L) {
            return RecsQueryRes()
        }

        val defs: List<EntityRef>
        val defsTime = measureTimeMillis {
            defs = camundaRepositoryService
                .createProcessDefinitionQuery()
                .applyPredicate(predicate)
                .applySort(recsQuery)
                .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                    EntityRef.create(AppName.EPROC, ID, it.id)
                }
        }

        log.debug { "$ID total count time: $totalCountTime, defs time: $defsTime" }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(defs)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }


    private fun ProcessDefinitionQuery.applyPredicate(pred: Predicate): ProcessDefinitionQuery {
        val engineQuery = PredicateUtils.convertToDto(pred, EngineDefQuery::class.java)

        val keyPredicate: ValuePredicate? = PredicateUtils.mapValuePredicates(pred) { valuePred ->
            if (valuePred.getAttribute() == KEY_ATT) {
                valuePred
            } else {
                null
            }
        } as ValuePredicate?

        return this.apply {
            if (engineQuery.onlyLatestVersion) {
                latestVersion()
            }

            if (engineQuery.key.isNotBlank() && keyPredicate != null) {
                if (keyPredicate.getType() == ValuePredicate.Type.EQ) {
                    processDefinitionKey(keyPredicate.getValue().asText())
                } else if (keyPredicate.getType() == ValuePredicate.Type.LIKE) {
                    processDefinitionKeyLike("%${keyPredicate.getValue().asText()}%")
                }
            }
        }
    }

    private fun ProcessDefinitionQuery.applySort(recsQuery: RecordsQuery): ProcessDefinitionQuery {
        if (recsQuery.sortBy.isEmpty()) {
            return this.apply {
                orderByProcessDefinitionKey()
                asc()
            }
        }

        val sortBy = recsQuery.sortBy[0]
        return this.apply {
            if (sortBy.attribute == VERSION_ATT) {
                orderByProcessDefinitionVersion()
                if (sortBy.ascending) {
                    asc()
                } else {
                    desc()
                }
            }
        }
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        val atts: List<ProcessDefinitionEngineRecord>
        val attsTime = measureTimeMillis {
            atts = camundaRepositoryService
                .createProcessDefinitionQuery()
                .processDefinitionIdIn(*recordIds.toTypedArray())
                .list()
                .map {
                    val entity = it as ProcessDefinitionEntity
                    ProcessDefinitionEngineRecord(entity)
                }
                .sortByIds(recordIds)
        }

        log.debug { "$ID get atts time: $attsTime, size: ${recordIds.size}" }
        return atts
    }

    private inner class ProcessDefinitionEngineRecord(
        private val defEntity: ProcessDefinitionEntity,

        val id: String = defEntity.id,
        val key: String = defEntity.key,
        val deploymentId: String = defEntity.deploymentId,
        val version: Int = defEntity.version,
        val name: String = defEntity.name ?: ""
    ) : IdentifiableRecord {

        @AttName("ecosDefRev")
        fun getEcosDefRev(): EntityRef {
            val ecosDefRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)

            return ecosDefRev?.let {
                EntityRef.create(
                    AppName.EPROC,
                    BpmnProcessDefVersionRecords.ID,
                    it.id.toString()
                )
            } ?: EntityRef.EMPTY
        }

        @AttName("overallStatistics")
        fun getOverallStatistics(): BpmnProcessStatistics {
            if (key.isBlank()) {
                BpmnProcessStatistics(0, 0)
            }
            val incidentsCount = camundaRuntimeService.createIncidentQuery()
                .processDefinitionKeyIn(key)
                .count()
            val instancesCount = camundaRuntimeService.createProcessInstanceQuery()
                .processDefinitionKey(key)
                .count()
            return BpmnProcessStatistics(incidentsCount, instancesCount)
        }

        @AttName("statistics")
        fun getStatistics(): BpmnProcessStatistics {
            if (id.isBlank()) {
                BpmnProcessStatistics(0, 0)
            }

            val incidentsCount = camundaRuntimeService.createIncidentQuery()
                .processDefinitionId(id)
                .count()
            val instancesCount = camundaRuntimeService.createProcessInstanceQuery()
                .processDefinitionId(id)
                .count()
            return BpmnProcessStatistics(incidentsCount, instancesCount)
        }

        @AttName("activityStatistics")
        fun getActivityStatistics(): List<ActivityStatistics> {
            val stat = managementService.createActivityStatisticsQuery(id)
                .includeIncidents()
                .list()
            return stat.map {
                ActivityStatistics(
                    activityId = it.id,
                    instances = it.instances.toLong(),
                    incidentStatistics = it.incidentStatistics.map { incident ->
                        IncidentStatistics(
                            type = incident.incidentType,
                            count = incident.incidentCount.toLong()
                        )
                    }
                )
            }
        }

        @AttName(".disp")
        fun getDisp(): String {
            return key
        }

        override fun getIdentificator(): String {
            return id
        }
    }
}

data class EngineDefQuery(
    var onlyLatestVersion: Boolean = true,
    var key: String = ""
)
