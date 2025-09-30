package ru.citeck.ecos.process.domain.proc.repo.edata

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao

@Configuration
class EcosDataProcStateConfig(
    private val dbDomainFactory: DbDomainFactory
) {

    companion object {
        const val PROC_STATE_REPO_SRC_ID = "proc-instance-state-repo"
        const val PROC_INSTANCE_REPO_SRC_ID = "proc-instance-repo"
        val PROC_INSTANCE_REPO_TYPE = ModelUtils.getTypeRef("proc-instance")
        val PROC_STATE_REPO_TYPE = ModelUtils.getTypeRef("proc-instance-state")
    }

    @Bean
    fun procInstanceRepoRecordsDao(): RecordsDao {
        val dao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create()
                        .withId(PROC_INSTANCE_REPO_SRC_ID)
                        .withTypeRef(PROC_INSTANCE_REPO_TYPE)
                        .withAuthEnabled(true)
                        .build()
                )
                .withDataService(
                    DbDataServiceConfig.create()
                        .withTable("process_instance")
                        .withStoreTableMeta(true)
                        .build()
                )
                .build()
        ).withPermsComponent(ProcDefPerms)
            .withSchema("public")
            .build()

        return dao
    }

    @Bean
    fun procInstanceStateRepoRecordsDao(): RecordsDao {
        val dao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create()
                        .withId(PROC_STATE_REPO_SRC_ID)
                        .withTypeRef(PROC_STATE_REPO_TYPE)
                        .withAuthEnabled(true)
                        .build()
                )
                .withDataService(
                    DbDataServiceConfig.create()
                        .withTable("process_instance_state")
                        .withStoreTableMeta(true)
                        .build()
                )
                .build()
        ).withPermsComponent(ProcDefPerms)
            .withSchema("public")
            .build()

        return dao
    }

    private object ProcDefPerms : DbPermsComponent {
        override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
            val isAdmin = authorities.contains(AuthRole.ADMIN)
            return object : DbRecordPerms {
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
                    return isAdmin
                }

                override fun hasReadPerms(): Boolean {
                    return true
                }

                override fun hasWritePerms(): Boolean {
                    return isAdmin
                }
            }
        }
    }
}
