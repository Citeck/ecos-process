package ru.citeck.ecos.process.domain.dmn.api.records

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.DecisionDefinition
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class DmnDecisionRecords(
    private val camundaRepositoryService: RepositoryService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "dmn-decision"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val count = camundaRepositoryService
            .createDecisionDefinitionQuery()
            .count()

        val decisions = camundaRepositoryService
            .createDecisionDefinitionQuery()
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.id)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(decisions)
        result.setTotalCount(count)
        result.setHasMore(count > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        return camundaRepositoryService
            .createDecisionDefinitionQuery()
            .decisionDefinitionIdIn(*recordIds.toTypedArray())
            .list()
            .map {
                it.toDecisionRecord()
            }
    }

    private fun DecisionDefinition.toDecisionRecord() = DecisionRecord(
        id = id,
        key = key,
        definition = EntityRef.create(
            AppName.EPROC,
            DMN_DEF_RECORDS_SOURCE_ID,
            resourceName.substringBefore(DMN_RESOURCE_NAME_POSTFIX)
        ),
        version = version,
        name = name
    )

    private inner class DecisionRecord(
        val id: String,
        val key: String? = "",
        val definition: EntityRef = EntityRef.EMPTY,
        val version: Int,
        val name: String? = ""
    )
}
