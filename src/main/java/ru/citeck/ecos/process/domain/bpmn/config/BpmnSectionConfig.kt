package ru.citeck.ecos.process.domain.bpmn.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.datasource.DbDataSourceImpl
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.dto.DbTableRef
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DefaultDbPermsComponent
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val recordsService: RecordsService
) {

    companion object {
        const val BPMN_SECTION_REPO_SOURCE_ID = "bpmn-section-repo"
    }

    @Bean
    fun bpmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {
        val dataSource = dataSourceManager.getDataSource("eproc", JdbcDataSource::class.java).getJavaDataSource()
        val permsComponent = DefaultDbPermsComponent(recordsService)

        val typeRef = TypeUtils.getTypeRef("bpmn-section")
        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(BPMN_SECTION_REPO_SOURCE_ID)
                        withTypeRef(typeRef)
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withAuthEnabled(false)
                        withTableRef(DbTableRef("ecos_data", "bpmn_section"))
                        withTransactional(false)
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withPermsComponent(permsComponent)
            .withDataSource(DbDataSourceImpl(dataSource))
            .build()
        return recordsDao
    }
}
