package ru.citeck.ecos.process.domain.proc.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateRepository
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.records3.RecordsService

@Configuration
class ProcInstanceRepoConfig {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Configuration
    class EcosDataConfig {

        init {
            log.info { "=== Initialize ecos-data repo for process instances ===" }
        }

        @Bean
        fun procInstanceRepository(
            recordsService: RecordsService,
            procStateRepo: EcosDataProcStateAdapter
        ): EcosDataProcInstanceAdapter {
            return EcosDataProcInstanceAdapter(recordsService, procStateRepo)
        }

        @Bean
        fun procStateRepository(
            recordsService: RecordsService,
            ecosDataProcDefRevAdapter: EcosDataProcDefRevAdapter
        ): EcosDataProcStateAdapter {
            return EcosDataProcStateAdapter(recordsService, ecosDataProcDefRevAdapter)
        }
    }

    @Configuration
    class MongoConfig {

        init {
            log.info { "=== Initialize mongo (legacy) repo for process instances ===" }
        }

        @Bean
        fun procInstanceMongoRepo(procInstanceRepo: MongoProcInstanceRepository): MongoProcInstanceAdapter {
            return MongoProcInstanceAdapter(procInstanceRepo)
        }

        @Bean
        fun procStateMongoRepo(procStateRepo: MongoProcStateRepository): MongoProcStateAdapter {
            return MongoProcStateAdapter(procStateRepo)
        }
    }
}
