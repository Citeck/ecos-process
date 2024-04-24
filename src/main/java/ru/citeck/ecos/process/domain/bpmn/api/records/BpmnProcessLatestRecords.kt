package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.BPMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getLatestProcessDefinitionsByKeys
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

// TODO: permissions and tests
/**
 * The source is used to select deployed process in engine.
 *
 * For example, in the BPMN editor, select called process in DMN element.
 *
 * return id as process key is important contract.
 */
@Component
class BpmnProcessLatestRecords(
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "bpmn-proc-latest"

        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, ProcessLatestQuery::class.java)

        val countQuery = camundaRepositoryService
            .createProcessDefinitionQuery()
            .latestVersion()
        if (query.definition.isNotBlank()) {
            countQuery.processDefinitionKeyLike("%${query.definition}%")
        }

        val count = countQuery.count()
        if (count == 0L) {
            return RecsQueryRes()
        }

        val processesQuery = camundaRepositoryService
            .createProcessDefinitionQuery()
            .latestVersion()
        if (query.definition.isNotBlank()) {
            processesQuery.processDefinitionKeyLike("%${query.definition}%")
        }

        val processes = processesQuery
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems).map {
                EntityRef.create(AppName.EPROC, ID, it.key)
            }

        val result = RecsQueryRes<EntityRef>()

        result.setRecords(processes)
        result.setTotalCount(count)
        result.setHasMore(count > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any?> {
        return camundaRepositoryService
            .getLatestProcessDefinitionsByKeys(recordIds)
            .map {
                it.toProcessLatestRecord()
            }
            .sortByIds(recordIds)
    }

    private fun ProcessDefinition.toProcessLatestRecord() = ProcessLatestRecord(
        id = key,
        definition = let {
            val processDefIdFromResourceName = resourceName.substringBefore(BPMN_RESOURCE_NAME_POSTFIX)

            val procDefId = processDefIdFromResourceName.ifBlank {
                val procDefRev = procDefService.getProcessDefRevByDeploymentId(deploymentId)
                procDefRev?.procDefId ?: ""
            }

            EntityRef.create(
                AppName.EPROC,
                BpmnProcessDefRecords.ID,
                procDefId
            )
        },
        version = version,
        name = name ?: key
    )

    private inner class ProcessLatestRecord(
        val id: String,
        val definition: EntityRef = EntityRef.EMPTY,
        val version: Int,
        val name: String? = ""
    ) : IdentifiableRecord {

        @AttName(".disp")
        fun getDisp(): MLText {
            return MLText(name ?: id)
        }

        @AttName("startFormRef")
        fun getStartFormRef(): EntityRef {
            val startFormRefFromDef = recordsService.getAtt(definition, "startFormRef?id").asText()
            return EntityRef.valueOf(startFormRefFromDef)
        }

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "bpmn-proc-latest")
        }

        override fun getIdentificator(): String {
            return id
        }
    }

    data class ProcessLatestQuery(
        var definition: String = ""
    )
}
