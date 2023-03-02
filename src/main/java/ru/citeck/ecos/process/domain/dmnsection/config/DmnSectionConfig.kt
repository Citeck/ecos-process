package ru.citeck.ecos.process.domain.dmnsection.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.datasource.DbDataSourceImpl
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.dto.DbTableRef
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.dmnsection.eapps.DMN_SECTION_TYPE
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

const val DMN_SECTION_REPO_SOURCE_ID = "dmn-section-repo"

@Configuration
class DmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory
) {

    @Bean
    fun dmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {
        val dataSource = dataSourceManager.getDataSource(AppName.EPROC, JdbcDataSource::class.java, true)

        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(DMN_SECTION_REPO_SOURCE_ID)
                        withTypeRef(TypeUtils.getTypeRef(DMN_SECTION_TYPE))
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withAuthEnabled(false)
                        withTableRef(DbTableRef("ecos_data", "dmn_section"))
                        withTransactional(false)
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withDataSource(DbDataSourceImpl(dataSource))
            .build()

        return recordsDao
    }
}
