package ru.citeck.ecos.process.domain.bpmn.kpi

import org.springframework.stereotype.Service
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

const val BPMN_KPI_VALUE_SOURCE_ID = "bpmn-kpi-value"

interface BpmnKpiService {

    fun createKpiValue(value: BpmnKpiValue)

    fun queryKpiValues(kpiSettingsRef: EntityRef, processRef: EntityRef): List<EntityRef>
}

@Service
class BpmnKpiServiceImpl(
    private val recordsService: RecordsService
) : BpmnKpiService {

    companion object {
        private val log = mu.KotlinLogging.logger {}
    }

    override fun createKpiValue(value: BpmnKpiValue) {
        log.trace { "Create BpmnKpiValue: \n${value.toPrettyString()}" }

        val atts = mapOf(
            "kpiSettingsRef" to value.settingsRef,
            "value" to value.value,
            "processInstanceRef" to value.processInstanceRef,
            "processRef" to value.processRef,
            "procDefRef" to value.procDefRef,
            "document" to value.document,
            "documentTypeRef" to value.documentType,
            "sourceBpmnActivityId" to value.sourceBpmnActivityId,
            "targetBpmnActivityId" to value.targetBpmnActivityId
        )
        recordsService.create("${AppName.EMODEL}/$BPMN_KPI_VALUE_SOURCE_ID", atts)
    }

    override fun queryKpiValues(kpiSettingsRef: EntityRef, processRef: EntityRef): List<EntityRef> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId("${AppName.EMODEL}/$BPMN_KPI_VALUE_SOURCE_ID")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(
                    Predicates.and(
                        Predicates.eq("kpiSettingsRef", kpiSettingsRef),
                        Predicates.eq("processRef", processRef)
                    )
                )
            }
        ).getRecords()
    }
}

data class BpmnKpiValue(
    val settingsRef: EntityRef,
    val value: Number,
    val processInstanceRef: EntityRef,
    val processRef: EntityRef,
    val procDefRef: EntityRef,
    val document: EntityRef,
    val documentType: EntityRef,
    val sourceBpmnActivityId: String?,
    val targetBpmnActivityId: String?
)

enum class BpmnKpiType {
    DURATION,
    COUNT
}

enum class BpmnKpiEventType {
    START,
    END
}

enum class BpmnDurationKpiTimeType {
    WORKING,
    CALENDAR
}
