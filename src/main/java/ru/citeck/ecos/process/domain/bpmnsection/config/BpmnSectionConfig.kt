package ru.citeck.ecos.process.domain.bpmnsection.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.common.section.perms.RootSectionPermsComponent
import ru.citeck.ecos.process.common.section.records.SectionParentMixin
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.lib.perms.EcosPermissionsService
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsComponent
import ru.citeck.ecos.webapp.lib.spring.context.data.DbPermsCalculatorComponent
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

const val BPMN_SECTION_REPO_SOURCE_ID = "bpmn-section-repo"

@Configuration
class BpmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val permsService: EcosPermissionsService,
    private val customRecordPermsComponent: CustomRecordPermsComponent
) {

    @Bean
    fun bpmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {

        val typeRef = ModelUtils.getTypeRef("bpmn-section")
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
                        withTable("bpmn_section")
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withPermsComponent(createPermsComponent())
            .withSchema("ecos_data")
            .build()

        recordsDao.addAttributesMixin(SectionParentMixin(BpmnSectionsProxyDao.SOURCE_ID))

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
