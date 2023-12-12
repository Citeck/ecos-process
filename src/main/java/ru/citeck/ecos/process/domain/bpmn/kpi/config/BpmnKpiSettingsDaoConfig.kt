package ru.citeck.ecos.process.domain.bpmn.kpi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnKpiSettingsDaoConfig(
    private val dbDomainFactory: DbDomainFactory
) {
    companion object {
        const val SOURCE_ID = "bpmn-kpi-settings"

        private const val TYPE_ID = "bpmn-kpi-settings"
    }

    @Bean
    fun bpmnDurationKpiDao(dataSourceManager: EcosDataSourceManager): RecordsDao {

        val typeRef = ModelUtils.getTypeRef(TYPE_ID)
        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(SOURCE_ID)
                        withTypeRef(typeRef)
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withTable("bpmn_duration_kpi_settings")
                        withStoreTableMeta(true)
                    }
                )
                .build()
        )
            .withSchema("ecos_data")
            .build()

        return recordsDao
    }
}
