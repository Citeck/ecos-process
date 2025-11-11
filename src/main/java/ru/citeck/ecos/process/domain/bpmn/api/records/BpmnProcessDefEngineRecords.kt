package ru.citeck.ecos.process.domain.bpmn.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.process.ActivityStatistics
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessStatistics
import ru.citeck.ecos.process.domain.bpmn.process.IncidentStatistics
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
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
    private val camundaRuntimeService: RuntimeService,
    private val workspaceService: WorkspaceService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "bpmn-def-engine"

        private const val KEY_ATT = "key"
        private const val VERSION_ATT = "version"

        fun createRef(id: String): EntityRef {
            return EntityRef.create(AppName.EPROC, ID, id)
        }
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        return if (AuthContext.isRunAsSystemOrAdmin()) {
            queryAsSuperUser(recsQuery)
        } else {
            queryAsRegularUser(recsQuery)
        }
    }

    private fun queryAsSuperUser(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
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

        log.debug { "$ID super user, total count time: $totalCountTime, defs time: $defsTime" }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(defs)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    private fun queryAsRegularUser(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)

        val defs: List<EntityRef>
        val defsTime = measureTimeMillis {
            defs = camundaRepositoryService
                .createProcessDefinitionQuery()
                .applyPredicate(predicate)
                .applySort(recsQuery)
                .unlimitedList()
                .checkPermissionsAndReplaceToEmptyRecord()
                .filterIsInstance<ProcessDefinitionEntity>()
                .map {
                    EntityRef.create(AppName.EPROC, ID, it.id)
                }
        }

        log.debug { "$ID regular user, query count: ${defs.size}, defs time: $defsTime" }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(defs.drop(recsQuery.page.skipCount).take(recsQuery.page.maxItems))
        result.setTotalCount(defs.size.toLong())
        result.setHasMore(defs.size > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    private fun ProcessDefinitionQuery.applyPredicate(pred: Predicate): ProcessDefinitionQuery {
        val engineQuery = PredicateUtils.convertToDto(pred, EngineDefQuery::class.java)
        val keyValue = engineQuery.key.replace("%", "").trim()

        check(engineQuery.onlyLatestVersion || keyValue.isNotBlank()) {
            "All version query supported only with key attribute"
        }

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

            keyPredicate?.let {
                if (keyValue.isNotBlank()) {
                    if (keyPredicate.getType() == ValuePredicate.Type.EQ) {
                        processDefinitionKey(keyValue)
                    } else if (keyPredicate.getType() == ValuePredicate.Type.LIKE) {
                        processDefinitionKeyLike("%$keyValue%")
                    }
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

    private fun MutableList<ProcessDefinition>.checkPermissionsAndReplaceToEmptyRecord(): MutableList<Any> {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return this.toMutableList()
        }

        val result: MutableList<Any>
        val checkPermissionTime = measureTimeMillis {
            val deploymentIds = map { it.deploymentId }.toList()
            val ecosProcDefsWithDeploymentIds = procDefService.getProcessDefRevByDeploymentIds(deploymentIds)
            val ecosProcDefRefs = ecosProcDefsWithDeploymentIds.map {
                val localId = workspaceService.addWsPrefixToId(it.procDefId, it.workspace)
                EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
            }

            val deploymentsWithReadPerms = recordsService.getAtts(ecosProcDefRefs, HasReadPerms::class.java)
                .filter { it.hasReadInstancePerms && !it.deploymentId.isNullOrBlank() }
                .map { it.deploymentId }

            result = map { procDef ->
                if (deploymentsWithReadPerms.contains(procDef.deploymentId)) {
                    procDef
                } else {
                    EmptyIdentifiableRecord(procDef.id)
                }
            }.toMutableList()
        }

        log.debug { "$ID check permissions time: $checkPermissionTime" }

        return result
    }

    private class HasReadPerms {
        var id: String? = ""

        @AttName("deploymentId")
        var deploymentId: String? = ""

        @AttName("permissions._has.bpmn-process-instance-read?bool!")
        var hasReadInstancePerms: Boolean = false

        override fun toString(): String {
            return "HasReadPerms(id=$id, deploymentId=$deploymentId, hasReadInstancePerms=$hasReadInstancePerms)"
        }
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any?> {
        val atts: List<IdentifiableRecord?>
        val attsTime = measureTimeMillis {
            atts = camundaRepositoryService
                .createProcessDefinitionQuery()
                .processDefinitionIdIn(*recordIds.toTypedArray())
                .list()
                .checkPermissionsAndReplaceToEmptyRecord()
                .map {
                    when (it) {
                        is ProcessDefinitionEntity -> ProcessDefinitionEngineRecord(it)
                        is EmptyIdentifiableRecord -> it
                        else -> error("Unknown record type: ${it.javaClass}")
                    }
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

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "bpmn-def-engine")
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
