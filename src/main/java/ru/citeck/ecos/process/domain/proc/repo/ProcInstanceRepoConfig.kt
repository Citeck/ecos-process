package ru.citeck.ecos.process.domain.proc.repo

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.EcosWebAppApi

@Configuration
class ProcInstanceRepoConfig(
    val webAppApi: EcosWebAppApi
) {

    @Configuration
    @ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "false")
    class EcosDataConfig {

        @Bean
        fun procInstanceRepository(recordsService: RecordsService, procStateRepo: ProcStateRepository): ProcInstanceRepository {
            return EcosDataProcInstanceAdapter(recordsService, procStateRepo as EcosDataProcStateAdapter)
        }

        @Bean
        fun procStateRepository(
            recordsService: RecordsService,
            ecosDataProcDefRevAdapter: ProcDefRevRepository
        ): ProcStateRepository {
            return EcosDataProcStateAdapter(recordsService, ecosDataProcDefRevAdapter as EcosDataProcDefRevAdapter)
        }
    }

    @Configuration
    @ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "true")
    class MongoConfig {

        @Bean
        fun procInstanceRepository(procInstanceRepo: MongoProcInstanceRepository): ProcInstanceRepository {
            return MongoProcInstanceAdapter(procInstanceRepo)
        }

        @Bean
        fun procStateRepository(procStateRepo: MongoProcStateRepository): ProcStateRepository {
            return MongoProcStateAdapter(procStateRepo)
        }
    }
}
