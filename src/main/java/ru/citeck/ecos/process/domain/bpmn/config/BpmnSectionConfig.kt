package ru.citeck.ecos.process.domain.bpmn.config

import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.datasource.DbDataSourceImpl
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.dto.DbTableRef
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DefaultDbPermsComponent
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnSectionConfig(
    private val dbDomainFactory: DbDomainFactory,
    private val ecosWebAppContext: EcosWebAppContext,
    private val recordsService: RecordsService
) {

    companion object {
        const val BPMN_SECTION_SOURCE_ID = "bpmn-section"
        const val BPMN_SECTION_REPO_SOURCE_ID = "$BPMN_SECTION_SOURCE_ID-repo"
        val log = KotlinLogging.logger {}
    }

    @Bean
    fun bpmnSectionRepoDao(dataSourceManager: EcosDataSourceManager): RecordsDao {
        val dataSource = dataSourceManager.getDataSource("eproc", JdbcDataSource::class.java).getJavaDataSource()
        val permsComponent = DefaultDbPermsComponent(recordsService)

        val typeRef = TypeUtils.getTypeRef("bpmn-section")
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
                        withAuthEnabled(false)
                        withTableRef(DbTableRef("ecos_data", "bpmn_section"))
                        withTransactional(false)
                        withStoreTableMeta(true)
                    }
                )
                .build()
        ).withPermsComponent(permsComponent)
            .withDataSource(DbDataSourceImpl(dataSource))
            .build()
        return recordsDao
    }

    @Bean
    fun bpmnSectionsProxyDao(): RecordsDao {
        val recordsDao = object : RecordsDaoProxy(BPMN_SECTION_SOURCE_ID, BPMN_SECTION_REPO_SOURCE_ID) {
            override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

                val eprocRecords = super.queryRecords(recsQuery)

                if (isAlfrescoAvailable()) {
                    val alfRecords = queryBpmnSectionsFromAlfresco()
                    return mergeResults(alfRecords, eprocRecords)
                }
                return eprocRecords
            }

            override fun delete(recordsId: List<String>): List<DelStatus> {
                return super.delete(recordsId)
            }
        }
        return recordsDao
    }

    private fun isAlfrescoAvailable(): Boolean {
        return ecosWebAppContext.getWebAppsApi().isAppAvailable("alfresco")
    }

    private fun queryBpmnSectionsFromAlfresco(): RecsQueryRes<*> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId("alfresco/")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(
                    Predicates.and(
                        Predicates.eq("parent", "workspace://SpacesStore/ecos-bpm-category-root"),
                        Predicates.eq("assocName", "cm:subcategories")
                    )
                )
            },
            mapOf(
                "name" to "cm:title"
            )
        )
    }

    private fun mergeResults(alfRecords: RecsQueryRes<*>?, eprocRecords: RecsQueryRes<*>?): RecsQueryRes<*>? {
        return eprocRecords
//        TODO("Not yet implemented")
    }
}
