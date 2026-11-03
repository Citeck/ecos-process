package ru.citeck.ecos.process.domain.proctask.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao

const val TASK_SYNC_JOB_SOURCE = "bpmn-task-atts-sync-job"

@Configuration
class ProcTaskSyncJobConfig(private val dbDomainFactory: DbDomainFactory) {

    @Bean
    fun procTaskSyncJobDao(): RecordsDao {
        return dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(TASK_SYNC_JOB_SOURCE)
                        withTypeRef(ModelUtils.getTypeRef(TASK_SYNC_JOB_SOURCE))
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withTable("bpmn_task_atts_sync_job")
                        withStoreTableMeta(true)
                    }
                ).build()
        ).withSchema("ecos_data")
            .withPermsComponent(object : DbPermsComponent {
                override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
                    val admin = authorities.contains(AuthRole.ADMIN)
                    return object : DbRecordPerms {
                        override fun getAdditionalPerms(): Set<String> = emptySet()
                        override fun getAuthoritiesWithReadPermission(): Set<String> = setOf(AuthRole.ADMIN)
                        override fun hasAttReadPerms(name: String): Boolean = admin
                        override fun hasAttWritePerms(name: String): Boolean = admin
                        override fun hasReadPerms(): Boolean = admin
                        override fun hasWritePerms(): Boolean = admin
                    }
                }
            }).build()
    }
}
