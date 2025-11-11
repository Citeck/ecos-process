package ru.citeck.ecos.process.common.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.procdef.repo.mongo.MongoProcDefRepoAdapter
import ru.citeck.ecos.webapp.api.constants.WebAppProfile
import ru.citeck.ecos.webapp.lib.env.EcosWebAppEnvironment
import ru.citeck.ecos.webapp.lib.patch.local.EcosLocalPatchRunner

@Component
class EcosDataMigrationState(
    private val localPatchRunner: EcosLocalPatchRunner,
    private val optionalMongoRepo: ObjectProvider<MongoProcDefRepoAdapter>,
    private val environment: EcosWebAppEnvironment
) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    private var edataStoragePrimary: Boolean? = null

    @Value("\${ecos-process.repo.mongo.enabled}")
    private var mongoRepoEnabledByProperty: Boolean = false

    @Synchronized
    fun setMigrationPatchExecuted(value: Boolean) {
        if (!mongoRepoEnabledByProperty && value) {
            edataStoragePrimary = true
            log.info { "===== ECOS DATA storage was enabled! =====" }
        }
    }

    @Synchronized
    fun isEdataStoragePrimary(): Boolean {
        var edataStoragePrimary = edataStoragePrimary
        if (edataStoragePrimary == null) {
            edataStoragePrimary = if (optionalMongoRepo.ifAvailable == null) {
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
            log.info { "===== ECOS DATA storage is primary: $edataStoragePrimary =====" }
            this.edataStoragePrimary = edataStoragePrimary
        }
        return edataStoragePrimary
    }
}
