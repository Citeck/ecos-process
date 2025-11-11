package ru.citeck.ecos.process.common.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.proc.repo.mongo.MongoProcInstanceAdapter
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepoAdapter
import ru.citeck.ecos.webapp.api.constants.WebAppProfile
import ru.citeck.ecos.webapp.lib.env.EcosWebAppEnvironment
import ru.citeck.ecos.webapp.lib.patch.local.EcosLocalPatchRunner
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class EcosDataMigrationState(
    private val localPatchRunner: EcosLocalPatchRunner,
    private val optionalProcDefMongoRepo: ObjectProvider<MongoProcDefRepoAdapter>,
    private val optionalProcInstanceMongoRepo: ObjectProvider<MongoProcInstanceAdapter>,
    private val environment: EcosWebAppEnvironment
) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    private var edataStoragePrimaryForProcDefs: Boolean? = null
    private var edataStoragePrimaryForProcInstances: Boolean? = null

    @Value("\${ecos-process.repo.mongo.enabled}")
    private var mongoRepoEnabledByProperty: Boolean = false

    private val lock = ReentrantLock()

    fun setProcDefMigrationCompleted(value: Boolean) {
        lock.withLock {
            if (!mongoRepoEnabledByProperty && value) {
                edataStoragePrimaryForProcDefs = true
                log.info { "===== Process def migration completed. ECOS DATA storage was enabled! =====" }
            }
        }
    }

    fun setProcInstancesMigrationCompleted(value: Boolean) {
        lock.withLock {
            if (!mongoRepoEnabledByProperty && value) {
                edataStoragePrimaryForProcInstances = true
                log.info { "===== Process instances migration completed. ECOS DATA storage was enabled! =====" }
            }
        }
    }

    fun isEdataStoragePrimaryForProcDefs(): Boolean {
        return isEdataStoragePrimary(
            "proc-defs",
            optionalProcDefMongoRepo,
            { edataStoragePrimaryForProcDefs },
            { edataStoragePrimaryForProcDefs = it }
        )
    }

    fun isEdataStoragePrimaryForProcInstances(): Boolean {
        return isEdataStoragePrimary(
            "proc-instances",
            optionalProcInstanceMongoRepo,
            { edataStoragePrimaryForProcInstances },
            { edataStoragePrimaryForProcInstances = it }
        )
    }

    private inline fun isEdataStoragePrimary(
        type: String,
        mongoRepo: ObjectProvider<*>,
        crossinline getFlag: () -> Boolean?,
        crossinline setFlag: (Boolean) -> Unit
    ): Boolean {
        lock.withLock {
            var edataStoragePrimary = getFlag()
            if (edataStoragePrimary == null) {
                edataStoragePrimary = if (mongoRepo.ifAvailable == null) {
                    true
                } else {
                    if (environment.acceptsProfiles(WebAppProfile.TEST)) {
                        !mongoRepoEnabledByProperty
                    } else {
                        !mongoRepoEnabledByProperty && localPatchRunner.isLocalPatchExecuted(
                            MongoToEcosDataMigrationConfig.MongoToEcosDataMigration::class.java
                        )
                    }
                }
                log.info { "===== ECOS DATA storage is primary for '$type': $edataStoragePrimary =====" }
                setFlag(edataStoragePrimary)
            }
            return edataStoragePrimary
        }
    }
}
