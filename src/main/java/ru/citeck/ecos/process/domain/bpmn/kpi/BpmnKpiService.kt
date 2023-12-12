package ru.citeck.ecos.process.domain.bpmn.kpi

import org.springframework.stereotype.Service
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

const val BPMN_KPI_VALUE_SOURCE_ID = "bpmn-kpi-value"

interface BpmnKpiService {

    fun createKpiValue(value: BpmnKpiValue)

    fun queryKpiValues(kpiSettingsRef: EntityRef, processId: String): List<EntityRef>
}

@Service
class BpmnKpiServiceImpl(
    private val recordsService: RecordsService
) : BpmnKpiService {

    override fun createKpiValue(value: BpmnKpiValue) {
        val atts = mapOf(
            "kpiSettingsRef" to value.settingsRef,
            "value" to value.value,
            "processInstanceId" to value.processInstanceId,
            "processId" to value.processId
        )
        recordsService.create("${AppName.EMODEL}/$BPMN_KPI_VALUE_SOURCE_ID", atts)
    }

    override fun queryKpiValues(kpiSettingsRef: EntityRef, processId: String): List<EntityRef> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId("${AppName.EMODEL}/$BPMN_KPI_VALUE_SOURCE_ID")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(
                    Predicates.and(
                        Predicates.eq("kpiSettingsRef", kpiSettingsRef),
                        Predicates.eq("processId", processId)
                    )
                )
            }
        ).getRecords()
    }
}

data class BpmnKpiValue(
    val settingsRef: EntityRef,
    val value: Number,
    val processInstanceId: String,
    val processId: String
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
