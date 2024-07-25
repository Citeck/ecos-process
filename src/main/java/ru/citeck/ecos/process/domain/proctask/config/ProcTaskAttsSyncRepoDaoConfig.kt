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
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncMixin
import ru.citeck.ecos.records3.record.dao.RecordsDao

const val PROC_TASK_ATTS_SYNC_TYPE = "bpmn-task-atts-sync"
const val PROC_TASK_ATTS_SYNC_SOURCE_ID = PROC_TASK_ATTS_SYNC_TYPE
const val PROC_TASK_ATTS_SYNC_REPO_SOURCE_ID = "$PROC_TASK_ATTS_SYNC_TYPE-repo"
val PROC_TASK_ATTS_SYNC_REPO_TYPE_REF = ModelUtils.getTypeRef(PROC_TASK_ATTS_SYNC_TYPE)

const val PROC_TASK_ATTS_SYNC_ATTS = "attributesSync"

@Configuration
class ProcTaskAttsSyncRepoDaoConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val procTaskAttsSyncMixin: ProcTaskAttsSyncMixin
) {

    @Bean
    fun procTaskAttsSyncRepoDao(): RecordsDao {
        val dao = dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create()
                        .withId(PROC_TASK_ATTS_SYNC_REPO_SOURCE_ID)
                        .withTypeRef(PROC_TASK_ATTS_SYNC_REPO_TYPE_REF)
                        .build()
                )
                .withDataService(
                    DbDataServiceConfig.create()
                        .withTable("ecos_bpmn_task_atts_sync")
                        .withStoreTableMeta(true)
                        .build()
                )
                .build()
        )
            .withPermsComponent(AttsSyncPerms)
            .withSchema("ecos_data")
            .build()

        dao.addAttributesMixin(procTaskAttsSyncMixin)

        return dao
    }

    private object AttsSyncPerms : DbPermsComponent {
        override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
            val isAdmin = authorities.contains(AuthRole.ADMIN)
            return object : DbRecordPerms {
                override fun getAdditionalPerms(): Set<String> {
                    return emptySet()
                }

                override fun getAuthoritiesWithReadPermission(): Set<String> {
                    return setOf(AuthRole.ADMIN)
                }

                override fun hasAttReadPerms(name: String): Boolean {
                    return isAdmin
                }

                override fun hasAttWritePerms(name: String): Boolean {
                    return isAdmin
                }

                override fun hasReadPerms(): Boolean {
                    return isAdmin
                }

                override fun hasWritePerms(): Boolean {
                    return isAdmin
                }
            }
        }
    }
}
