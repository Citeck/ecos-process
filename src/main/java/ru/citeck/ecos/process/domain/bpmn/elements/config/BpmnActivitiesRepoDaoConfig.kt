package ru.citeck.ecos.process.domain.bpmn.elements.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.data.sql.datasource.DbDataSourceImpl
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.dto.DbTableRef
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsMixin
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnActivitiesRepoDaoConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val bpmnProcessElementsMixin: BpmnProcessElementsMixin
) {

    @Bean
    fun bpmnActivitiesRepoDao(eventsService: EventsService, dataSourceManager: EcosDataSourceManager): RecordsDao {
        val accessPerms = object : DbRecordPerms {
            override fun getAuthoritiesWithReadPermission(): Set<String> {
                return setOf(AuthGroup.EVERYONE)
            }

            override fun isCurrentUserHasWritePerms(): Boolean {
                return false
            }

            override fun isCurrentUserHasAttReadPerms(name: String): Boolean {
                return true
            }

            override fun isCurrentUserHasAttWritePerms(name: String): Boolean {
                return false
            }
        }
        val permsComponent = object : DbPermsComponent {
            override fun getRecordPerms(recordRef: EntityRef): DbRecordPerms {
                return accessPerms
            }
        }

        val dataSource = dataSourceManager.getDataSource("eproc", JdbcDataSource::class.java).getJavaDataSource()

        val typeRef = TypeUtils.getTypeRef("bpmn-process-element")
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
                        withAuthEnabled(false)
                        withTableRef(DbTableRef("ecos_data", "bpmn_process_elements"))
                        withTransactional(false)
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withPermsComponent(permsComponent)
            .withDataSource(DbDataSourceImpl(dataSource))
            .build()

        recordsDao.addAttributesMixin(bpmnProcessElementsMixin)

        return recordsDao
    }
}
