package ru.citeck.ecos.process.domain.procdef.repo.edata

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
class EcosDataProcDefConfig(
    private val dbDomainFactory: DbDomainFactory
) {

    companion object {
        const val PROC_DEF_REPO_SRC_ID = "proc-def-repo"
        const val PROC_DEF_REV_REPO_SRC_ID = "proc-def-rev-repo"
        val PROC_DEF_REPO_TYPE = ModelUtils.getTypeRef("proc-def")
        val PROC_DEF_REV_REPO_TYPE = ModelUtils.getTypeRef("proc-def-rev")
    }

    @Bean
    fun procDefRepoRecordsDao(): RecordsDao {
        val dao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create()
                        .withId(PROC_DEF_REPO_SRC_ID)
                        .withTypeRef(PROC_DEF_REPO_TYPE)
                        .withAuthEnabled(true)
                        .build()
                )
                .withDataService(
                    DbDataServiceConfig.create()
                        .withTable("process_def")
                        .withStoreTableMeta(true)
                        .build()
                )
                .build()
        )
            .withPermsComponent(ProcDefPerms)
            .withSchema("public")
            .build()

        return dao
    }

    @Bean
    fun procDefRevRepoRecordsDao(): RecordsDao {
        val dao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create()
                        .withId(PROC_DEF_REV_REPO_SRC_ID)
                        .withTypeRef(PROC_DEF_REV_REPO_TYPE)
                        .withAuthEnabled(true)
                        .build()
                )
                .withDataService(
                    DbDataServiceConfig.create()
                        .withTable("process_def_rev")
                        .withStoreTableMeta(true)
                        .build()
                )
                .build()
        )
            .withPermsComponent(ProcDefPerms)
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
