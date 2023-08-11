package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getLatestProcessDefinitionsByKeys
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessLatestRecords(
    private val camundaRepositoryService: RepositoryService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "bpmn-proc-latest"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val count = camundaRepositoryService
            .createProcessDefinitionQuery()
            .latestVersion()
            .count()

        val processes = camundaRepositoryService
            .createProcessDefinitionQuery()
            .latestVersion()
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.key)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(processes)
        result.setTotalCount(count)
        result.setHasMore(count > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        return camundaRepositoryService
            .getLatestProcessDefinitionsByKeys(recordIds)
            .map {
                it.toProcessLatestRecord()
            }
    }

    private fun ProcessDefinition.toProcessLatestRecord() = ProcessLatestRecord(
        id = key,
        definition = EntityRef.create(
            AppName.EPROC,
            BPMN_PROCESS_DEF_RECORDS_SOURCE_ID,
            resourceName.substringBefore(BPMN_RESOURCE_NAME_POSTFIX)
        ),
        version = version,
        name = name ?: key
    )

    private inner class ProcessLatestRecord(
        val id: String,
        val definition: EntityRef = EntityRef.EMPTY,
        val version: Int,
        val name: String? = ""
    )
}
