package ru.citeck.ecos.process.domain.bpmn.elements.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

@Component
class BpmnProcessElementsDao : RecordsDaoProxy(
    BPMN_ELEMENTS_SOURCE_ID,
    BPMN_ELEMENTS_REPO_SOURCE_ID
) {

    companion object {
        const val BPMN_ELEMENTS_SOURCE_ID = "bpmn-process-elements"
        const val BPMN_ELEMENTS_REPO_SOURCE_ID = "$BPMN_ELEMENTS_SOURCE_ID-repo"
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return super.queryRecords(recsQuery)
        }
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val newPredicate = PredicateUtils.mapValuePredicates(predicate) { pred ->
            if (pred.getAttribute() == "procDefRef") {
                preProcessProcDefQueryAtt(RecordRef.valueOf(pred.getValue().asText()))
            } else {
                pred
            }
        } ?: Predicates.alwaysTrue()
        return super.queryRecords(recsQuery.copy { withQuery(newPredicate) })
    }

    private fun preProcessProcDefQueryAtt(value: RecordRef): Predicate {
        return if (value.appName == "alfresco") {
            val procDefId = recordsService.getAtt(value, "ecosbpm:processId").asText()
            Predicates.and(
                Predicates.eq("procDefId", procDefId),
                Predicates.eq("engine", "flowable")
            )
        } else {
            Predicates.and(
                Predicates.eq("procDefId", value.id),
                Predicates.not(Predicates.eq("engine", "flowable"))
            )
        }
    }
}
