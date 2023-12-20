package ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders

import mu.KotlinLogging
import ru.citeck.ecos.process.common.toDecisionKey
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.dmn.service.EcosDmnService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

interface BpmnKpiStakeholdersFinder {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun getRecordsService(): RecordsService

    fun getEcosDmnService(): EcosDmnService

    fun searchStakeholders(
        processId: String,
        document: EntityRef,
        activityId: String,
        eventType: BpmnKpiEventType,
        kpiType: BpmnKpiType
    ): List<BpmnKpiSettings> {
        // TODO: replace after bugfix in ecos-webapp-spring-base-parent 1.50+ ECOSCOM-5071
        // val processRef = processId.toEntityRef(AppName.EPROC, BpmnProcessLatestRecords.ID)

        var processRef = processId.toEntityRef()
        if (processRef.getAppName().isBlank()) {
            processRef = processRef.withAppName(AppName.EPROC)
        }
        if (processRef.getSourceId().isBlank()) {
            processRef = processRef.withSourceId(BpmnProcessLatestRecords.ID)
        }

        return getRecordsService().query(
            RecordsQuery.create {
                withSourceId(BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(
                    Predicates.and(
                        Predicates.eq("processRef", processRef),
                        Predicates.eq("enabled", true),
                        Predicates.eq("kpiType", kpiType.name),
                        Predicates.eq("targetBpmnActivityId", activityId),
                        Predicates.eq("targetBpmnActivityEvent", eventType.name)
                    )
                )
                withPage(QueryPage(10_000, 0, null))
            },
            BpmnKpiSettings::class.java
        ).getRecords()
            .filter {
                it.dmnConditionSatisfied(document)
            }
    }

    private fun BpmnKpiSettings.dmnConditionSatisfied(documentRef: EntityRef): Boolean {
        if (EntityRef.isEmpty(dmnCondition)) {
            return true
        }

        val dmnConditionRef = dmnCondition!!
        val model = getRecordsService().getAtt(dmnConditionRef, "definition.model")
            .asMap(String::class.java, String::class.java)
        val filledModel = mutableMapOf<String, Any?>()

        getRecordsService().getAtts(documentRef, model).forEach { key, attr ->
            filledModel[key] = attr.asJavaObj()
        }

        val result = getEcosDmnService().evaluateDecisionByKeyAndCollectMapEntries(
            dmnConditionRef.toDecisionKey(),
            filledModel
        )
        if (result.isEmpty()) {
            log.debug { "Decision result is empty for kpi settings: ${this.id}" }
            return false
        }

        val decisionResult = result.values.first().first().toString().toBoolean()
        log.debug { "Decision result for kpi settings: ${this.id} is $decisionResult" }

        return decisionResult
    }
}

class BpmnKpiSettings(
    var id: String = "",

    var name: String? = "",
    var kpiType: BpmnKpiType? = null,
    var enabled: Boolean = false,
    var processRef: EntityRef? = EntityRef.EMPTY,

    var dmnCondition: EntityRef? = EntityRef.EMPTY,
    var model: Map<String, String>? = emptyMap(),

    var sourceBpmnActivityId: String? = "",
    var sourceBpmnActivityEvent: BpmnKpiEventType? = null,

    var targetBpmnActivityId: String? = "",
    var targetBpmnActivityEvent: BpmnKpiEventType? = null,

    var durationKpi: String? = "",
    var durationKpiTimeType: BpmnDurationKpiTimeType? = null,
    var kpiAsNumber: Long? = null,

    var countKpi: Long? = null,
    var countPeriod: String? = ""
) {

    fun getRef(): EntityRef {
        return if (id.isNotBlank()) {
            EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, id)
        } else {
            EntityRef.EMPTY
        }
    }
}
