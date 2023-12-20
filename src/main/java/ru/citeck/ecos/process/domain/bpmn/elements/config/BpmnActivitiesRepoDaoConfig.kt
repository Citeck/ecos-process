package ru.citeck.ecos.process.domain.bpmn.elements.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsMixin
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

const val BPMN_PROCESS_ELEMENT_TYPE = "bpmn-process-element"

@Configuration
class BpmnActivitiesRepoDaoConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val bpmnProcessElementsMixin: BpmnProcessElementsMixin
) {

    @Bean
    fun bpmnActivitiesRepoDao(eventsService: EventsService, dataSourceManager: EcosDataSourceManager): RecordsDao {

        // TODO: add permissions based on BpmnProcDef
        val accessPerms = object : DbRecordPerms {

            override fun getAdditionalPerms(): Set<String> {
                return emptySet()
            }

            override fun getAuthoritiesWithReadPermission(): Set<String> {
                return setOf(AuthGroup.EVERYONE)
            }
            override fun hasAttReadPerms(name: String): Boolean {
                return true
            }
            override fun hasAttWritePerms(name: String): Boolean {
                return false
            }
            override fun hasReadPerms(): Boolean {
                return true
            }
            override fun hasWritePerms(): Boolean {
                return false
            }
        }
        val permsComponent = object : DbPermsComponent {
            override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
                return accessPerms
            }
        }

        val typeRef = ModelUtils.getTypeRef(BPMN_PROCESS_ELEMENT_TYPE)
        val recordsDao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                        withTypeRef(typeRef)
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withTable("bpmn_process_elements")
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withSchema("ecos_data")
            .withPermsComponent(permsComponent)
            .build()

        recordsDao.addAttributesMixin(bpmnProcessElementsMixin)

        return recordsDao
    }
}
