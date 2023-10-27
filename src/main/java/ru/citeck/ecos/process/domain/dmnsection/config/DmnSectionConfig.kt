package ru.citeck.ecos.process.domain.dmnsection.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.process.common.section.perms.RootSectionPermsComponent
import ru.citeck.ecos.process.common.section.records.SectionParentMixin
import ru.citeck.ecos.process.common.section.records.SectionsProxyDao
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.lib.perms.EcosPermissionsService
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsComponent
import ru.citeck.ecos.webapp.lib.spring.context.data.DbPermsCalculatorComponent
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class DmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val permsService: EcosPermissionsService,
    private val customRecordPermsComponent: CustomRecordPermsComponent
) {

    companion object {
        const val TYPE_ID = "dmn-section"
        const val SOURCE_ID = "dmn-section"
        const val REPO_SOURCE_ID = "$SOURCE_ID-repo"
    }

    @Bean
    fun dmnSectionDao(): RecordsDao {
        return SectionsProxyDao(SOURCE_ID, REPO_SOURCE_ID, SectionType.DMN)
    }

    @Bean
    fun dmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {

        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(REPO_SOURCE_ID)
                        withTypeRef(ModelUtils.getTypeRef(TYPE_ID))
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withTable("dmn_section")
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withSchema("ecos_data")
            .withPermsComponent(createPermsComponent())
            .build()

        recordsDao.addAttributesMixin(SectionParentMixin(SOURCE_ID))

        return recordsDao
    }

    private fun createPermsComponent(): DbPermsComponent {
        val permsCalc = permsService.createCalculator()
            .withoutDefaultComponents()
            .addComponent(RootSectionPermsComponent())
            .addComponent(customRecordPermsComponent)
            .allowAllForAdmins()
            .build()
        return DbPermsCalculatorComponent(permsCalc)
    }
}
