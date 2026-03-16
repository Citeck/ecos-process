package ru.citeck.ecos.process.domain.bpmn.process.delayed

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao

@Configuration
class BpmnDelayedStartConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val recordsService: RecordsService
) {

    @Bean
    fun bpmnDelayedStartCmdDao(): RecordsDao {
        val permsComponent = object : DbPermsComponent {
            override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
                val isAdmin = authorities.contains(AuthRole.ADMIN)
                val isCompleted = !recordsService.getAtt(record, BpmnDelayedStartCmdDesc.ATT_COMPLETED_AT).isEmpty()
                return object : DbRecordPerms {
                    override fun getAdditionalPerms(): Set<String> = emptySet()
                    override fun getAuthoritiesWithReadPermission(): Set<String> = setOf(AuthRole.ADMIN)
                    override fun hasAttReadPerms(name: String): Boolean = isAdmin
                    override fun hasAttWritePerms(name: String): Boolean = isAdmin && !isCompleted
                    override fun hasReadPerms(): Boolean = isAdmin
                    override fun hasWritePerms(): Boolean = isAdmin && !isCompleted
                }
            }
        }

        return dbDomainFactory.create(
            DbDomainConfig.create()
                .withRecordsDao(
                    DbRecordsDaoConfig.create {
                        withId(BpmnDelayedStartCmdDesc.BPMN_DELAYED_START_CMD_SOURCE_ID)
                        withTypeRef(BpmnDelayedStartCmdDesc.BPMN_DELAYED_START_CMD_TYPE_REF)
                    }
                )
                .withDataService(
                    DbDataServiceConfig.create {
                        withTable("bpmn_delayed_start_cmd")
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withSchema("ecos_data")
            .withPermsComponent(permsComponent)
            .build()
    }
}
