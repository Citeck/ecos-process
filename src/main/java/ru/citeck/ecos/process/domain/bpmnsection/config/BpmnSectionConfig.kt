package ru.citeck.ecos.process.domain.bpmnsection.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.process.common.section.perms.RootSectionPermsComponent
import ru.citeck.ecos.process.common.section.records.SectionChildrenPermsUpdater
import ru.citeck.ecos.process.common.section.records.SectionParentMixin
import ru.citeck.ecos.process.common.section.records.SectionsProxyDao
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.lib.perms.EcosPermissionsService
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsComponent
import ru.citeck.ecos.webapp.lib.spring.context.data.DbPermsCalculatorComponent
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val permsService: EcosPermissionsService,
    private val customRecordPermsComponent: CustomRecordPermsComponent
) {
    companion object {
        const val TYPE_ID = "bpmn-section"
        const val SOURCE_ID = "bpmn-section"
        private const val REPO_SOURCE_ID = "$SOURCE_ID-repo"
    }

    @Bean
    fun bpmnSectionChildrenPermsUpdater(
        eventsService: EventsService,
        recordsService: RecordsService
    ): SectionChildrenPermsUpdater {
        return SectionChildrenPermsUpdater(eventsService, recordsService, SectionType.BPMN)
    }

    @Bean
    fun bpmnSectionDao(): RecordsDao {
        return SectionsProxyDao(SOURCE_ID, REPO_SOURCE_ID, SectionType.BPMN)
    }

    @Bean
    fun bpmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {

        val typeRef = ModelUtils.getTypeRef(TYPE_ID)
        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(REPO_SOURCE_ID)
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
