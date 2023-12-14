package ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.dmn.service.EcosDmnService
import ru.citeck.ecos.records3.RecordsService

@Component
class BpmnKpiDefaultStakeholdersFinder(
    private val recordsService: RecordsService,
    private val ecosDmnService: EcosDmnService
) : BpmnKpiStakeholdersFinder {

    override fun getRecordsService(): RecordsService {
        return recordsService
    }

    override fun getEcosDmnService(): EcosDmnService {
        return ecosDmnService
    }
}
