package ru.citeck.ecos.process.common.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepo
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRevRepo
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosLocalPatch
import java.time.Instant
import java.util.concurrent.Callable

@Configuration
@ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "false")
class MongoToEcosDataMigrationConfig {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Bean
    fun mongoToEcosDataMigration(
        mongoProcDefRepo: ObjectProvider<MongoProcDefRepo>,
        mongoProcDefRevRepo: ObjectProvider<MongoProcDefRevRepo>,
        mongoProcInstanceRepo: ObjectProvider<MongoProcInstanceRepository>,
        mongoProcStateRepo: ObjectProvider<MongoProcStateRepository>,
        edataProcDefRepository: ProcDefRepository,
        edataProcDefRevRepository: ProcDefRevRepository,
        edataProcInstanceRepo: ProcInstanceRepository,
        edataProcStateRepo: ProcStateRepository
    ): MongoToEcosDataMigration {
        return MongoToEcosDataMigration(
            mongoProcDefRepo.getIfAvailable(),
            mongoProcDefRevRepo.getIfAvailable(),
            mongoProcInstanceRepo.getIfAvailable(),
            mongoProcStateRepo.getIfAvailable(),
            edataProcDefRepository,
            edataProcDefRevRepository,
            edataProcInstanceRepo,
            edataProcStateRepo
        )
    }

    @EcosLocalPatch("mongo-to-ecos-data-migration", "2025-09-30T00:00:02Z")
    class MongoToEcosDataMigration(
        private val mongoProcDefRepo: MongoProcDefRepo?,
        private val mongoProcDefRevRepo: MongoProcDefRevRepo?,
        private val mongoProcInstanceRepo: MongoProcInstanceRepository?,
        private val mongoProcStateRepository: MongoProcStateRepository?,
        private val edataProcDefRepository: ProcDefRepository,
        private val edataProcDefRevRepository: ProcDefRevRepository,
        private val edataProcInstanceRepo: ProcInstanceRepository,
        private val edataProcStateRepo: ProcStateRepository
    ) : Callable<DataValue> {

        override fun call(): DataValue {
            return DataValue.createObj()
                .set("migratedProcDefs", migrateProcDefs())
                .set("migratedProcDefRevsCount", migrateProcDefRevs())
                .set("migratedProcInstancesCount", migrateProcInstances())
                .set("migratedProcStatesCount", migrateProcStates())
        }

        private fun migrateProcDefs(): List<String> {

            if (mongoProcDefRepo == null) {
                log.info { "Mongo proc def repo is not available. Migration will be skipped" }
                return emptyList()
            }

            val startedAt = System.currentTimeMillis()
            log.info { "Start proc-def migration" }

            val migratedProcesses = mutableListOf<String>()

            fun Instant?.orEpoch() = this ?: Instant.EPOCH

            forEach(mongoProcDefRepo) { mongoProcDef ->
                val procType = mongoProcDef.procType ?: return@forEach
                val extId = mongoProcDef.extId ?: return@forEach
                val existing = edataProcDefRepository.findByIdInWs("", procType, extId)
                val procKey = mongoProcDef.procType + '$' + mongoProcDef.extId
                if (existing == null || existing.modified.orEpoch() < mongoProcDef.modified.orEpoch()) {
                    log.info { "Run migration for '$procKey'" }
                    if (existing != null && existing.id != mongoProcDef.id) {
                        log.info {
                            "Process def for '$procKey' was found, but id doesn't match. " +
                                "Delete existing definition to "
                        }
                        edataProcDefRepository.delete(existing)
                    }
                    edataProcDefRepository.save(mongoProcDef)
                    migratedProcesses.add(procKey)
                } else {
                    log.info { "Migration of '$procKey' doesn't required" }
                }
            }

            log.info {
                "proc-def migration completed in ${System.currentTimeMillis() - startedAt} ms. " +
                    "Migrated count: ${migratedProcesses.size}"
            }

            return migratedProcesses
        }

        private fun migrateProcDefRevs(): Int {
            return migrateEntities(
                "proc-def-revs", mongoProcDefRevRepo,
                { it.id },
                { it.created },
                { edataProcDefRevRepository.findById(it) },
                { edataProcDefRevRepository.save(it) }
            )
        }

        private fun migrateProcInstances(): Int {
            return migrateEntities(
                "proc-instances",
                mongoProcInstanceRepo,
                { it.id },
                { it.created ?: Instant.EPOCH },
                { edataProcInstanceRepo.findById(it) },
                { edataProcInstanceRepo.save(it) }
            )
        }

        private fun migrateProcStates(): Int {
            return migrateEntities(
                "proc-states",
                mongoProcStateRepository,
                { it.id },
                { it.created },
                { edataProcStateRepo.findById(it) },
                { edataProcStateRepo.save(it) }
            )
        }

        private fun <T : Any> migrateEntities(
            type: String,
            repo: MongoRepository<T, EntityUuid>?,
            getId: (T) -> EntityUuid?, getCreated: (T) -> Instant,
            findExistingById: (EntityUuid) -> T?,
            saveMigratedEntity: (T) -> Unit
        ): Int {

            if (repo == null) {
                log.info { "Mongo '$type' repo is not available. Migration will be skipped" }
                return 0
            }

            val startedAt = System.currentTimeMillis()

            log.info { "Start '$type' migration" }
            var migratedCount = 0

            forEach(repo) { mongoEntity ->
                val entityId = getId(mongoEntity) ?: return@forEach
                val existing = findExistingById(entityId)
                if (existing == null || getCreated(existing) < getCreated(mongoEntity)) {
                    migratedCount++
                    saveMigratedEntity(mongoEntity)
                }
            }

            log.info {
                "'$type' migration completed in ${System.currentTimeMillis() - startedAt} ms. " +
                    "Migrated count: $migratedCount"
            }
            return migratedCount
        }

        private fun <T : Any> forEach(repo: MongoRepository<T, EntityUuid>, action: (T) -> Unit) {
            var pageReq = PageRequest.of(0, 20, Sort.Direction.ASC, "created")
            var pageData = repo.findAll(pageReq)
            while (!pageData.isEmpty) {
                pageData.content.forEach(action)
                pageReq = pageReq.next()
                pageData = repo.findAll(pageReq)
            }
        }
    }
}


