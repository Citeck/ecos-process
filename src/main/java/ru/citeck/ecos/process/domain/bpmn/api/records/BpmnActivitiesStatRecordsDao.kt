package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.utils.MandatoryParam
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import kotlin.math.sin
import kotlin.random.Random

@Component
class BpmnActivitiesStatRecordsDao : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        const val ID = "bpmn-activities-stat"

        private val ELEMENT_TYPES_TO_STAT = setOf(
            "startEvent",
            "userTask",
            "endEvent",
            "sequenceFlow",
            "serviceTask",
            "sendTask",
            "receiveTask"
        )
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language != "predicate-with-data") {
            return null
        }
        val queryData = recsQuery.getQuery(QueryDataWithPredicate::class.java)
        val procDef = queryData.data.procDef
        MandatoryParam.check("procDef", procDef) { RecordRef.isNotEmpty(it) }

        val definition = loadDefinition(procDef)
        val process = definition.rootElement[0].value as TProcess

        val activities = ArrayList<ActivityElement>()
        var iterator = 0
        process.flowElement.map {
            if (ELEMENT_TYPES_TO_STAT.contains(it.name.localPart)) {
                activities.add(ActivityElement(
                    id = it.value.id ?: "",
                    name = it.value.name ?: "",
                    activeCount = (Random.nextInt(300) * (1 + sin(iterator * 0.1f))).toInt(),
                    completedCount = (Random.nextInt(300) * (1 + sin(iterator * 0.1f))).toInt(),
                    type = it.name.localPart
                ))
                iterator++
            }
        }

        return activities
    }

    private fun loadDefinition(procDef: RecordRef): TDefinitions {

        val definitionsString = if (procDef.id.contains("$")) {
            loadDefinitionsFromAlfresco(procDef.id)
        } else {
            recordsService.getAtt(procDef, "definition?str").asText()
        }

        if (definitionsString.isBlank()) {
            error("Definition is blank. ProcDef: $procDef")
        }

        return BpmnXmlUtils.readFromString(definitionsString)
    }

    private fun loadDefinitionsFromAlfresco(procDefId: String): String {

        val engineAndProcId = procDefId.split("$")

        val predicateForAlfresco = Predicates.and(
            Predicates.eq("type", "ecosbpm:processModel"),
            Predicates.eq("ecosbpm:engine", "flowable"),
            Predicates.eq("ecosbpm:processId", engineAndProcId[1])
        )

        return recordsService.queryOne(
            RecordsQuery.create {
                withSourceId("alfresco/")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicateForAlfresco)
            }, "cm:content?str"
        ).asText()
    }

    override fun getId() = ID

    class QueryDataWithPredicate {
        var data: QueryData = QueryData()
        var predicate: Predicate = VoidPredicate.INSTANCE
    }

    class QueryData {
        var procDef: RecordRef = RecordRef.EMPTY
    }

    class ActivityElement(
        val id: String,
        val name: String,
        val activeCount: Int,
        val completedCount: Int,
        val type: String
    )
}
