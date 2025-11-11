package ru.citeck.ecos.process.common.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository
import ru.citeck.beans.BeanUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcInstanceAdapter
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcStateAdapter
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcStateRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefAdapter
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
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
import kotlin.math.max

@Configuration
@Profile("!test")
class MongoToEcosDataMigrationConfig {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val TYPE_PROC_DEF = "proc-def"
        private const val TYPE_PROC_DEF_REV = "proc-def-rev"
        private const val TYPE_PROC_INSTANCE = "proc-instance"
        private const val TYPE_PROC_INSTANCE_STATE = "proc-instance-state"

        private val TYPES_TO_CHECK = listOf(
            TYPE_PROC_DEF,
            TYPE_PROC_DEF_REV,
            TYPE_PROC_INSTANCE,
            TYPE_PROC_INSTANCE_STATE
        )
        private val ENTITY_BY_TYPE = mapOf(
            TYPE_PROC_DEF to ProcDefEntity::class,
            TYPE_PROC_DEF_REV to ProcDefRevEntity::class,
            TYPE_PROC_INSTANCE to ProcessInstanceEntity::class,
            TYPE_PROC_INSTANCE_STATE to ProcessStateEntity::class
        )

        private const val BATCH_SIZE = 20

        private val migrationContext = ThreadLocal<Boolean>()

        fun isMigrationContext(): Boolean {
            return migrationContext.get() == true
        }

        private val isRuntimeShutdownInitiated = AtomicBoolean(false)
        init {
            Runtime.getRuntime().addShutdownHook(
                thread(start = false) {
                    isRuntimeShutdownInitiated.set(true)
                }
            )
        }
    }

    @Bean
    fun mongoToEcosDataMigration(
        mongoTemplate: ObjectProvider<MongoTemplate>,
        mongoProcDefRepo: ObjectProvider<MongoProcDefRepo>,
        mongoProcDefRevRepo: ObjectProvider<MongoProcDefRevRepo>,
        mongoProcInstanceRepo: ObjectProvider<MongoProcInstanceRepository>,
        mongoProcStateRepo: ObjectProvider<MongoProcStateRepository>,
        edataProcDefRepository: EcosDataProcDefAdapter,
        edataProcDefRevRepository: EcosDataProcDefRevAdapter,
        edataProcInstanceRepo: EcosDataProcInstanceAdapter,
        edataProcStateRepo: EcosDataProcStateAdapter,
        ecosTypesRegistry: EcosTypesRegistry,
        ecosDataMigrationState: EcosDataMigrationState
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
            ecosTypesRegistry,
            ecosDataMigrationState
        )
    }

    @EcosPatchDependsOnApps(AppName.EMODEL)
    @EcosLocalPatch("mongo-to-ecos-data-migration2", "2025-09-30T00:00:07Z", afterStart = true)
    class MongoToEcosDataMigration(
        private val mongoTemplate: MongoTemplate?,
        private val mongoProcDefRepo: MongoProcDefRepo?,
        private val mongoProcDefRevRepo: MongoProcDefRevRepo?,
        private val mongoProcInstanceRepo: MongoProcInstanceRepository?,
        private val mongoProcStateRepository: MongoProcStateRepository?,
        private val edataProcDefRepository: EcosDataProcDefAdapter,
        private val edataProcDefRevRepository: EcosDataProcDefRevAdapter,
        private val edataProcInstanceRepo: EcosDataProcInstanceAdapter,
        private val edataProcStateRepo: EcosDataProcStateAdapter,
        private val ecosTypesRegistry: EcosTypesRegistry,
        private val ecosDataMigrationState: EcosDataMigrationState
    ) : Callable<DataValue> {

        override fun call(): DataValue {
            return run(emptyList(), false)
        }

        fun run(resetMigratedStateFor: List<String>, traceLogs: Boolean): DataValue {
            migrationContext.set(true)
            try {
                ecosTypesRegistry.initializationPromise().get(Duration.ofMinutes(100))
                for (typeId in TYPES_TO_CHECK) {
                    if (ecosTypesRegistry.getValue(typeId) == null) {
                        log.error { "Type doesn't exists in types registry: $typeId" }
                    }
                }
                resetMigratedState(resetMigratedStateFor)

                val result = DataValue.createObj()
                    .set("migratedProcDefs", migrateProcDefs(traceLogs))
                    .set("migratedProcDefRevsCount", migrateProcDefRevs(traceLogs))

                ecosDataMigrationState.setProcDefMigrationCompleted(true)

                log.info { "= Run final proc-def migrations" }
                // additional migration to process records which may be created or updated after
                // main migration was completed, but before migrationPatchExecuted flag become true.
                migrateProcDefs(traceLogs)
                migrateProcDefRevs(traceLogs)
                log.info { "= Final proc-def migrations completed" }

                @Suppress("ReplaceGetOrSet")
                result.set("migratedProcInstancesCount", migrateProcInstances())
                    .set("migratedProcStatesCount", migrateProcStates())

                ecosDataMigrationState.setProcInstancesMigrationCompleted(true)

                return result
            } finally {
                migrationContext.set(false)
            }
        }

        private fun migrateProcDefs(traceLogs: Boolean): List<String> {

            if (mongoProcDefRepo == null) {
                log.info { "Mongo proc def repo is not available. Migration will be skipped" }
                return emptyList()
            }

            val startedAt = System.currentTimeMillis()
            log.info { "Start proc-def migration" }

            val migratedProcesses = mutableListOf<String>()

            fun Instant?.orEpoch() = this ?: Instant.EPOCH

            forEach(mongoProcDefRepo) { procDefsBatch ->
                TxnContext.doInNewTxn {
                    for (mongoProcDef in procDefsBatch) {
                        val procType = mongoProcDef.procType ?: return@doInNewTxn
                        val extId = mongoProcDef.extId ?: return@doInNewTxn
                        val existing = edataProcDefRepository.findByIdInWs("", procType, extId)
                        val procKey = mongoProcDef.procType + '$' + mongoProcDef.extId
                        if (existing == null || existing.modified.orEpoch() < mongoProcDef.modified.orEpoch()) {
                            log.info { "Run migration for '$procKey'" }
                            if (existing != null && existing.id != mongoProcDef.id) {
                                log.info {
                                    "Process def for '$procKey' was found, but id doesn't match. " +
                                        "Delete existing definition"
                                }
                                edataProcDefRepository.delete(existing)
                            }
                            if (traceLogs) {
                                log.info { "Save following proc def: " + mongoProcDef.getAsJson() }
                            }
                            edataProcDefRepository.save(mongoProcDef)
                            migratedProcesses.add(procKey)
                        } else {
                            log.info { "Migration of '$procKey' doesn't required" }
                        }
                    }
                }
            }

            log.info {
                "proc-def migration completed in ${System.currentTimeMillis() - startedAt} ms. " +
                    "Migrated count: ${migratedProcesses.size}"
            }

            return migratedProcesses
        }

        private fun resetMigratedState(types: List<String>) {
            if (types.isEmpty()) {
                return
            }
            for (type in types) {
                val entityType = ENTITY_BY_TYPE[type]
                if (entityType == null) {
                    log.warn { "Entity type doesn't found for type '$type'" }
                    continue
                }
                val query = Query()
                val update = Update().set("migrated", false)

                log.info { "Reset migration state for type '$type'" }
                val result = mongoTemplate!!.updateMulti(query, update, entityType.java)
                log.info { "Matched: ${result.matchedCount}, Modified: ${result.modifiedCount}" }
            }
        }

        private fun migrateProcDefRevs(traceLogs: Boolean): Int {
            return migrateEntities(
                "proc-def-revs",
                mongoProcDefRevRepo,
                { it.id },
                { it.created },
                { edataProcDefRevRepository.findById(it) },
                {
                    if (traceLogs) {
                        log.info { "Save following proc def rev: " + it.getAsJson() }
                    }
                    edataProcDefRevRepository.save(it)
                }
            )
        }

        private fun migrateProcInstances(): Int {
            return migrateEntities(
                "proc-instances",
                mongoProcInstanceRepo,
                { it.id },
                { it.modified ?: Instant.EPOCH },
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
            getModified: (T) -> Instant,
            findExistingById: (EntityUuid) -> T?,
            saveMigratedEntity: (T) -> Unit
        ): Int {

            if (repo == null) {
                log.info { "Mongo '$type' repo is not available. Migration will be skipped" }
                return 0
            }

            if (mongoTemplate == null) {
                log.info { "MongoTemplate is not available. Migration will be skipped" }
                return 0
            }

            val startedAt = System.currentTimeMillis()

            log.info { "Start '$type' migration" }

            var skippedCount = 0
            var skippedCountLogIter = 0

            var migratedCount = 0
            var migratedCountLogIter = 0

            forEach(repo) { entitiesBatch ->
                TxnContext.doInNewTxn {
                    for (mongoEntity in entitiesBatch) {
                        val entityId = getId(mongoEntity) ?: return@doInNewTxn
                        val existing = findExistingById(entityId)
                        if (existing == null || getModified(existing) < getModified(mongoEntity)) {
                            saveMigratedEntity(mongoEntity)
                            val nextProcessedCountLogIter = (++migratedCount) / 100
                            if (migratedCountLogIter != nextProcessedCountLogIter) {
                                log.info { "Migrated $migratedCount records of type '$type'" }
                                migratedCountLogIter = nextProcessedCountLogIter
                            }
                        } else {
                            val nextSkippedCountIter = (++skippedCount) / 100
                            if (skippedCountLogIter != nextSkippedCountIter) {
                                log.info { "Skipped $skippedCount records of type '$type'" }
                                skippedCountLogIter = nextSkippedCountIter
                            }
                        }
                    }
                }
            }

            log.info {
                "'$type' migration completed in ${System.currentTimeMillis() - startedAt} ms. " +
                    "Migrated count: $migratedCount Skipped count: $skippedCount"
            }
            return migratedCount
        }

        private fun <T : Any> forEach(repo: MongoRepository<T, EntityUuid>, action: (List<T>) -> Unit) {

            val query = Query()
                .limit(BATCH_SIZE)
                .addCriteria(
                    Criteria().orOperator(
                        Criteria.where("migrated").ne(true)
                    )
                )

            val genericArgs = ReflectUtils.getGenericArgs(repo::class.java, MongoRepository::class.java)
            val entityType = genericArgs[0]
            var errorsCount = 0

            mongoTemplate!!

            fun findNextBatch(): List<T> {
                return doWithMongoTemplate(mongoTemplate, { errorsCount++ }) {
                    @Suppress("UNCHECKED_CAST")
                    it.find(query.limit(max(1, BATCH_SIZE / (errorsCount + 1))), entityType) as List<T>
                }
            }

            var entities = findNextBatch()
            while (entities.isNotEmpty()) {
                action(entities)
                var saveIterationInterruptedByError = false
                for (entity in entities) {
                    BeanUtils.setProperty(entity, "migrated", true)
                    try {
                        mongoTemplate.save(entity)
                    } catch (e: Throwable) {
                        log.error(e) { "Entity save failed. Let's process it in next iteration" }
                        Thread.sleep(10_000)
                        errorsCount++
                        saveIterationInterruptedByError = true
                        break
                    }
                }
                if (!saveIterationInterruptedByError) {
                    errorsCount = max(0, errorsCount - 1)
                }
                entities = findNextBatch()
            }
        }

        private fun <T> doWithMongoTemplate(
            mongoTemplate: MongoTemplate,
            onError: () -> Unit,
            action: (MongoTemplate) -> T
        ): T {
            var lastError: Throwable? = null
            fun handleError(e: Throwable) {
                onError.invoke()
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
