package ru.citeck.ecos.process.common.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.MongoRepository
import ru.citeck.beans.BeanUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepo
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRevRepo
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosLocalPatch
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatchDependsOnApps
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = ["ecos-process.repo.mongo.enabled"], havingValue = "false")
class MongoToEcosDataMigrationConfig {

    companion object {
        private val log = KotlinLogging.logger {}
        private val TYPES_TO_CHECK = listOf(
            "proc-def",
            "proc-def-rev",
            "proc-instance",
            "proc-instance-state"
        )

        private val migrationContext = ThreadLocal<Boolean>()

        fun isMigrationContext(): Boolean {
            return migrationContext.get() == true
        }

        private val isRuntimeShutdownInitiated = AtomicBoolean(false)
        init {
            Runtime.getRuntime().addShutdownHook(thread(start = false) {
                isRuntimeShutdownInitiated.set(true)
            })
        }
    }

    @Bean
    fun mongoToEcosDataMigration(
        mongoTemplate: ObjectProvider<MongoTemplate>,
        mongoProcDefRepo: ObjectProvider<MongoProcDefRepo>,
        mongoProcDefRevRepo: ObjectProvider<MongoProcDefRevRepo>,
        mongoProcInstanceRepo: ObjectProvider<MongoProcInstanceRepository>,
        mongoProcStateRepo: ObjectProvider<MongoProcStateRepository>,
        edataProcDefRepository: ProcDefRepository,
        edataProcDefRevRepository: ProcDefRevRepository,
        edataProcInstanceRepo: ProcInstanceRepository,
        edataProcStateRepo: ProcStateRepository,
        ecosTypesRegistry: EcosTypesRegistry
    ): MongoToEcosDataMigration {
        return MongoToEcosDataMigration(
            mongoTemplate.getIfAvailable(),
            mongoProcDefRepo.getIfAvailable(),
            mongoProcDefRevRepo.getIfAvailable(),
            mongoProcInstanceRepo.getIfAvailable(),
            mongoProcStateRepo.getIfAvailable(),
            edataProcDefRepository,
            edataProcDefRevRepository,
            edataProcInstanceRepo,
            edataProcStateRepo,
            ecosTypesRegistry
        )
    }

    @EcosPatchDependsOnApps(AppName.EMODEL)
    @EcosLocalPatch("mongo-to-ecos-data-migration", "2025-09-30T00:00:07Z", afterStart = true)
    class MongoToEcosDataMigration(
        private val mongoTemplate: MongoTemplate?,
        private val mongoProcDefRepo: MongoProcDefRepo?,
        private val mongoProcDefRevRepo: MongoProcDefRevRepo?,
        private val mongoProcInstanceRepo: MongoProcInstanceRepository?,
        private val mongoProcStateRepository: MongoProcStateRepository?,
        private val edataProcDefRepository: ProcDefRepository,
        private val edataProcDefRevRepository: ProcDefRevRepository,
        private val edataProcInstanceRepo: ProcInstanceRepository,
        private val edataProcStateRepo: ProcStateRepository,
        private val ecosTypesRegistry: EcosTypesRegistry
    ) : Callable<DataValue> {

        override fun call(): DataValue {
            migrationContext.set(true)
            try {
                ecosTypesRegistry.initializationPromise().get(Duration.ofMinutes(100))
                for (typeId in TYPES_TO_CHECK) {
                    if (ecosTypesRegistry.getValue(typeId) == null) {
                        log.error { "Type doesn't exists in types registry: $typeId" }
                    }
                }
                return DataValue.createObj()
                    .set("migratedProcDefs", migrateProcDefs())
                    .set("migratedProcDefRevsCount", migrateProcDefRevs())
                    .set("migratedProcInstancesCount", migrateProcInstances())
                    .set("migratedProcStatesCount", migrateProcStates())
            } finally {
                migrationContext.set(false)
            }
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
                "proc-def-revs",
                mongoProcDefRevRepo,
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
            getId: (T) -> EntityUuid?,
            getCreated: (T) -> Instant,
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

            val batchToSave = ArrayList<T>()
            val txnBatchSize = 100
            fun processBatch() {
                if (batchToSave.isEmpty()) {
                    return
                }
                TxnContext.doInNewTxn {
                    batchToSave.forEach { saveMigratedEntity(it) }
                }
                migratedCount += batchToSave.size
                batchToSave.clear()
                log.info { "Processed $migratedCount records of type '$type'" }
            }

            var skippedCount = 0
            var skippedCountLogIter = 0

            forEach(repo) { mongoEntity ->
                val entityId = getId(mongoEntity) ?: return@forEach
                val existing = TxnContext.doInNewTxn(true) { findExistingById(entityId) }
                if (existing == null || getCreated(existing) < getCreated(mongoEntity)) {
                    batchToSave.add(mongoEntity)
                } else {
                    val nextSkippedCountIter = (++skippedCount) / 100
                    if (skippedCountLogIter != nextSkippedCountIter) {
                        log.info { "Skipped $skippedCount records of type '$type'" }
                        skippedCountLogIter = nextSkippedCountIter
                    }
                }
                if (batchToSave.size >= txnBatchSize) {
                    processBatch()
                }
            }
            processBatch()

            log.info {
                "'$type' migration completed in ${System.currentTimeMillis() - startedAt} ms. " +
                    "Migrated count: $migratedCount Skipped count: $skippedCount"
            }
            return migratedCount
        }

        private fun <T : Any> forEach(repo: MongoRepository<T, EntityUuid>, action: (T) -> Unit) {

            if (mongoTemplate != null) {

                val query = Query()
                    .limit(100)
                    .addCriteria(
                        Criteria().orOperator(
                            Criteria.where("migrated").ne(true)
                        )
                    )

                val genericArgs = ReflectUtils.getGenericArgs(repo::class.java, MongoRepository::class.java)
                val entityType = genericArgs[0]

                var entities = doWithMongoTemplate(mongoTemplate) { it.find(query, entityType) }
                while (entities.isNotEmpty()) {
                    for (entity in entities) {
                        @Suppress("UNCHECKED_CAST")
                        action(entity as T)
                        BeanUtils.setProperty(entity, "migrated", true)
                        try {
                            mongoTemplate.save(entity)
                        } catch (e: Throwable) {
                            log.error(e) { "Entity save failed. Let's process it in next iteration" }
                            Thread.sleep(10_000)
                        }
                    }
                    entities = doWithMongoTemplate(mongoTemplate) { it.find(query, entityType) }
                }
            } else {
                var pageReq = PageRequest.of(0, 20, Sort.Direction.ASC, "created")
                var pageData = repo.findAll(pageReq)
                while (!pageData.isEmpty) {
                    pageData.content.forEach(action)
                    pageReq = pageReq.next()
                    pageData = repo.findAll(pageReq)
                }
            }
        }

        private fun <T> doWithMongoTemplate(mongoTemplate: MongoTemplate, action: (MongoTemplate) -> T): T {
            var lastError: Throwable? = null
            fun handleError(e: Throwable) {
                log.error(lastError) {
                    "Exception occurred while mongoTemplate usage. " +
                        "Sleep 10sec and try again"
                }
                if (e is InterruptedException || isRuntimeShutdownInitiated.get()) {
                    Thread.currentThread().interrupt()
                    if (e is InterruptedException) {
                        throw e
                    } else {
                        throw InterruptedException("Runtime shutdown initiated")
                    }
                }
                lastError = e
            }
            try {
                return action(mongoTemplate)
            } catch (e: Throwable) {
                handleError(e)
                while (true) {
                    try {
                        Thread.sleep(10_000)
                        return action(mongoTemplate)
                    } catch (e: Throwable) {
                        handleError(e)
                    }
                }
            }
        }
    }
}
