package ru.citeck.ecos.process.domain.procdef.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
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
    class EcosDataConfig {

        init {
            log.info { "=== Initialize ecos-data repo for process definitions ===" }
        }

        @Bean
        fun procDefRepository(
            recordsService: RecordsService,
            ecosDataProcDefRevAdapter: EcosDataProcDefRevAdapter,
            workspaceService: WorkspaceService
        ): EcosDataProcDefAdapter {
            return EcosDataProcDefAdapter(recordsService, ecosDataProcDefRevAdapter, workspaceService)
        }

        @Bean
        fun procDefRevRepository(recordsService: RecordsService): EcosDataProcDefRevAdapter {
            return EcosDataProcDefRevAdapter(recordsService)
        }
    }

    @Configuration
    @ConditionalOnExpression("'\${spring.data.mongodb.uri}'.startsWith('mongodb:')")
    class MongoConfig {

        init {
            log.info { "=== Initialize mongo (legacy) repo for process definitions ===" }
        }

        @Bean
        fun procDefMongoRepo(mongoProcDefRepo: MongoProcDefRepo): MongoProcDefRepoAdapter {
            return MongoProcDefRepoAdapter(mongoProcDefRepo)
        }

        @Bean
        fun procDefRevMongoRepo(procDefRevMongoRepo: MongoProcDefRevRepo): MongoProcDefRevRepoAdapter {
            return MongoProcDefRevRepoAdapter(procDefRevMongoRepo)
        }
    }
}
