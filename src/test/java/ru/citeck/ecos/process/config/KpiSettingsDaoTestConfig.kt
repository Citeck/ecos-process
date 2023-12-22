package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP
import ru.citeck.ecos.records3.record.dao.impl.mem.InMemDataRecordsDao

@Configuration
class KpiSettingsDaoTestConfig {

    @Bean
    fun createKpiSettingsRepoDao(): InMemDataRecordsDao {
        return InMemDataRecordsDao(BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP)
    }
}
