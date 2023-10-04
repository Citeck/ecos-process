package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessDefEngineRecords(
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "bpmn-def-engine"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val query = recsQuery.getQuery(EngineDefQuery::class.java)

        val countDefQueryBuilder = camundaRepositoryService
            .createProcessDefinitionQuery()
        if (query.onlyLatestVersion) {
            countDefQueryBuilder.latestVersion()
        }
        if (query.keyLike.isNotBlank()) {
            countDefQueryBuilder.processDefinitionKeyLike("%${query.keyLike}%")
        }

        val count = countDefQueryBuilder.count()


        val defQueryBuilder = camundaRepositoryService
            .createProcessDefinitionQuery()
        if (query.onlyLatestVersion) {
            defQueryBuilder.latestVersion()
        }
        if (query.keyLike.isNotBlank()) {
            defQueryBuilder.processDefinitionKeyLike("%${query.keyLike}%")
        }

        val processes = defQueryBuilder
            .orderByProcessDefinitionKey()
            .asc()
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.id)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(processes)
        result.setTotalCount(count)
        result.setHasMore(count > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        return camundaRepositoryService
            .createProcessDefinitionQuery()
            .processDefinitionIdIn(*recordIds.toTypedArray())
            .list()
            .map {
                val entity = it as ProcessDefinitionEntity
                entity.toProcessLatestRecord()
            }
    }

    private fun ProcessDefinitionEntity.toProcessLatestRecord(): ProcessDefinitionEngineRecord {
        val ecosDefRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)


        val record = ProcessDefinitionEngineRecord(
            id = id,
            key = key,
            deploymentId = deploymentId,
            ecosDefRev = ecosDefRev?.let {
                EntityRef.create(
                    AppName.EPROC,
                    BpmnProcessDefVersionRecords.ID,
                    it.id.toString()
                )
            } ?: EntityRef.EMPTY,
            version = version,
            name = name
        )

        return record
    }

    private inner class ProcessDefinitionEngineRecord(
        val id: String,
        val key: String,
        val deploymentId: String,
        val ecosDefRev: EntityRef = EntityRef.EMPTY,
        val version: Int,
        val name: String? = ""
    ) {


        @AttName("overallStatistics")
        fun getOverallStatistics(): BpmnProcessStatisticsValue {
            return BpmnProcessStatisticsValue(key)
        }
    }
}

private data class EngineDefQuery(
    val onlyLatestVersion: Boolean = true,
    val keyLike: String = ""
)
