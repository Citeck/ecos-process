package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnIncidentRecords(
    private val camundaRuntimeService: RuntimeService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "bpmn-incident"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, BpmnIncidentQuery::class.java)
        if (query.bpmnDefEngine.isEmpty()) {
            return RecsQueryRes()
        }


        val incidents = camundaRuntimeService.createIncidentQuery()
            .processDefinitionId(query.bpmnDefEngine.getLocalId())
            .list()



        val result = RecsQueryRes<EntityRef>()

        //result.setRecords(defs)
        //result.setTotalCount(totalCount)
        //result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<*>? {
        TODO("Not yet implemented")
    }
}

data class BpmnIncidentQuery(
    var bpmnDefEngine: EntityRef = EntityRef.EMPTY
)
