package ru.citeck.ecos.process.domain.proc.repo

import io.github.oshai.kotlinlogging.KotlinLogging
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

@Configuration
class ProcInstanceRepoConfig {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Configuration
    @ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "false")
    class EcosDataConfig {

        init {
            log.info { "=== Initialize ecos-data repo for process instances ===" }
        }

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

        init {
            log.info { "=== Initialize mongo (legacy) repo for process instances ===" }
        }

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
