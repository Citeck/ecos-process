package ru.citeck.ecos.process.domain.procdef.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefAdapter
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepo
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepoAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRevRepo
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRevRepoAdapter
import ru.citeck.ecos.records3.RecordsService

@Configuration
class ProcDefRepoConfig {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Configuration
    @ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "false")
    class EcosDataConfig {

        init {
            log.info { "=== Initialize ecos-data repo for process definitions ===" }
        }

        @Bean
        fun procDefRepository(
            recordsService: RecordsService,
            ecosDataProcDefRevAdapter: ProcDefRevRepository,
            workspaceService: WorkspaceService
        ): ProcDefRepository {
            val rev = ecosDataProcDefRevAdapter as EcosDataProcDefRevAdapter
            return EcosDataProcDefAdapter(recordsService, rev, workspaceService)
        }

        @Bean
        fun procDefRevRepository(recordsService: RecordsService): ProcDefRevRepository {
            return EcosDataProcDefRevAdapter(recordsService)
        }
    }

    @Configuration
    @ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "true")
    class MongoConfig {

        init {
            log.info { "=== Initialize mongo (legacy) repo for process definitions ===" }
        }

        @Bean
        fun procDefRepository(mongoProcDefRepo: MongoProcDefRepo): ProcDefRepository {
            return MongoProcDefRepoAdapter(mongoProcDefRepo)
        }

        @Bean
        fun procDefRevRepository(procDefRevMongoRepo: MongoProcDefRevRepo): ProcDefRevRepository {
            return MongoProcDefRevRepoAdapter(procDefRevMongoRepo)
        }
    }
}
