package ru.citeck.ecos.process.domain.dmn.api.records

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.DecisionDefinition
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.IdentifiableRecord
import ru.citeck.ecos.process.domain.bpmn.api.records.sortByIds
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getLatestDecisionDefinitionsByKeys
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class DmnDecisionLatestRecords(
    private val camundaRepositoryService: RepositoryService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "dmn-decision-latest"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val count = camundaRepositoryService
            .createDecisionDefinitionQuery()
            .latestVersion()
            .count()

        val decisions = camundaRepositoryService
            .createDecisionDefinitionQuery()
            .latestVersion()
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.key)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(decisions)
        result.setTotalCount(count)
        result.setHasMore(count > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any?> {
        return camundaRepositoryService
            .getLatestDecisionDefinitionsByKeys(recordIds)
            .map {
                it.toDecisionRecord()
            }
            .sortByIds(recordIds)
    }

    private fun DecisionDefinition.toDecisionRecord() = DecisionLatestRecord(
        id = key,
        definition = EntityRef.create(
            AppName.EPROC,
            DMN_DEF_RECORDS_SOURCE_ID,
            resourceName.substringBefore(DMN_RESOURCE_NAME_POSTFIX)
        ),
        version = version,
        name = name
    )

    private inner class DecisionLatestRecord(
        val id: String,
        val definition: EntityRef = EntityRef.EMPTY,
        val version: Int,
        val name: String? = ""
    ) : IdentifiableRecord {

        override fun getIdentificator(): String {
            return id
        }
    }
}
