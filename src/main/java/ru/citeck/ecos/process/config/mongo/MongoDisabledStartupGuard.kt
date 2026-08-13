package ru.citeck.ecos.process.config.mongo

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.apache.zookeeper.KeeperException
import org.camunda.bpm.engine.RepositoryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.patch.MongoToEcosDataMigrationConfig
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.lib.patch.local.EcosLocalPatchRunner
import ru.citeck.ecos.zookeeper.EcosZooKeeper
import java.time.Instant

/**
 * When MongoDB is disabled before the migration to ecos-data was completed, the application starts
 * without any error, but all previously created process definitions and instances become invisible.
 * This guard blocks such a start.
 *
 * The guard is registered as a `doBeforeAppReady` action with order=0f. The `mongo-to-ecos-data-migration2`
 * local patch is declared with `afterStart = true`, so it is executed from the `doWhenAppReady` queue,
 * which Ecos processes strictly after the whole `doBeforeAppReady` queue has completed. That ordering
 * guarantee - not this guard's own order value - is what ensures the guard always runs, and completes,
 * before the migration patch can start. order=0f is only the (default) position of this guard among
 * other `doBeforeAppReady` actions; since it is the default order and ties are broken by registration
 * order, it does not by itself put the guard ahead of every other action - and the guard's correctness
 * does not depend on that, only on the `afterStart` queue separation described above.
 *
 * On a clean installation (no Camunda deployments yet) there is nothing to migrate. That "migration is
 * not required" decision is persisted to ZooKeeper the first time it is made, because the migration
 * patch itself only starts (and only completes) well after the application has accepted traffic and
 * ecos-apps has had a chance to deploy BPMN artifacts. Without a persisted marker, a restart that
 * happens after deployments appear but before the patch has completed would see a non-empty deployment
 * count and refuse to start, with no way to recover: the patch can only run once the app is up, and
 * MongoDB may no longer be reachable at all in a mongo-disabled installation.
 *
 * Known limitation, accepted deliberately: the "clean installation" criterion relies EXCLUSIVELY on
 * the count of Camunda deployments (`camundaRepositoryService.createDeploymentQuery().count()`).
 * `RepositoryService.createDeployment()` is only invoked for BPMN and DMN. The CMMN proc-def type
 * (see [ru.citeck.ecos.process.domain.cmmn.api.records.CmmnProcDefRecords]) is stored in the very
 * same MongoDB-backed proc-def repository and is migrated by `mongo-to-ecos-data-migration2` just
 * like BPMN and DMN, but it never produces a Camunda deployment - so this guard cannot see it. A
 * hypothetical installation that uses only CMMN models (or simply has no BPMN process deployed yet)
 * would have an empty `ACT_RE_DEPLOYMENT`, be misclassified as "clean", and have the
 * migration-not-required marker persisted to ZooKeeper forever - after that, no later startup ever
 * re-evaluates the decision, even if MongoDB actually holds data that needs migrating. This has been
 * accepted as-is because no installation without at least one deployed BPMN process is known to exist;
 * documented here so the trade-off survives future maintenance.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(value = [MongoDisabledEnvironmentPostProcessor.MONGO_ENABLED_PROP], havingValue = "false")
class MongoDisabledStartupGuard(
    private val webAppApi: EcosWebAppApi,
    private val localPatchRunner: EcosLocalPatchRunner,
    private val camundaRepositoryService: RepositoryService,
    private val zooKeeper: EcosZooKeeper,
    @Value("\${ecos-process.repo.mongo.enabled:false}")
    private val mongoRepoEnabledByProperty: Boolean = false
) {

    companion object {
        private val log = KotlinLogging.logger {}

        const val MIGRATION_NOT_REQUIRED_MARKER_PATH = "/eproc/mongo/migration-not-required"
    }

    /**
     * Persistent marker written to ZooKeeper once it is established that a mongo-disabled installation
     * has no Camunda deployments and therefore nothing to migrate. Kept small and human-readable so an
     * operator inspecting the node can understand who created it and why.
     */
    data class MigrationNotRequiredMarker(
        val createdAt: Instant,
        val reason: String
    )

    @PostConstruct
    fun init() {
        webAppApi.doBeforeAppReady(0f) { checkMigrationCompleted() }
    }

    fun checkMigrationCompleted() {

        if (mongoRepoEnabledByProperty) {
            error(
                "Inconsistent configuration: ecos-process.mongo.enabled=false, but " +
                    "ecos-process.repo.mongo.enabled=true. The latter keeps MongoDB as the primary storage " +
                    "for process definitions and instances, which is impossible once MongoDB itself is " +
                    "disabled. Set ecos-process.repo.mongo.enabled=false (or leave it unset), or set " +
                    "ecos-process.mongo.enabled=true if MongoDB must remain the primary storage."
            )
        }

        val migrationExecuted = localPatchRunner.isLocalPatchExecuted(
            MongoToEcosDataMigrationConfig.MongoToEcosDataMigration::class.java
        )
        if (migrationExecuted) {
            log.info { "===== MongoDB is disabled. Migration to ECOS DATA is completed =====" }
            return
        }

        val marker = zooKeeper.getValue(MIGRATION_NOT_REQUIRED_MARKER_PATH, MigrationNotRequiredMarker::class.java)
        if (marker != null) {
            log.info {
                "===== MongoDB is disabled. Migration is not required: marker created at " +
                    "${marker.createdAt}, reason: '${marker.reason}' ====="
            }
            return
        }

        val deploymentsCount = camundaRepositoryService.createDeploymentQuery().count()
        if (deploymentsCount == 0L) {
            persistMigrationNotRequiredMarker()
            log.warn {
                "===== MongoDB is disabled. The 'mongo-to-ecos-data-migration2' patch has NOT been " +
                    "executed, but the Camunda deployment count (ACT_RE_DEPLOYMENT) is zero, so this " +
                    "installation is assumed to be clean and to have nothing to migrate. This decision is " +
                    "now persisted PERMANENTLY as a marker in ZooKeeper at $MIGRATION_NOT_REQUIRED_MARKER_PATH " +
                    "- every future startup will trust this marker and skip the deployment check entirely. " +
                    "NOTE: this check does not see CMMN process definitions, which live in the same MongoDB " +
                    "repository but never create a Camunda deployment. If this installation actually has " +
                    "data in MongoDB (BPMN, DMN or CMMN) that still needs migrating, that data is now at risk " +
                    "of becoming invisible: delete the marker node at $MIGRATION_NOT_REQUIRED_MARKER_PATH, set " +
                    "ecos-process.mongo.enabled=true and let the migration patch run before disabling MongoDB " +
                    "again ====="
            }
            return
        }

        error(
            "MongoDB is disabled (ecos-process.mongo.enabled=false), " +
                "but the 'mongo-to-ecos-data-migration2' patch was not executed and " +
                "there are $deploymentsCount deployments in Camunda. " +
                "Data from MongoDB would become invisible. Either: " +
                "(1) set ecos-process.mongo.enabled=true, wait until the migration patch is completed " +
                "and disable MongoDB after that; or " +
                "(2) if this is actually a clean installation with no real data in MongoDB (e.g. the " +
                "deployments are test/leftover artifacts), unblock the start by manually creating the " +
                "marker ZooKeeper node at the full path '/ecos$MIGRATION_NOT_REQUIRED_MARKER_PATH' " +
                "('ecos' is the ZooKeeper namespace, $MIGRATION_NOT_REQUIRED_MARKER_PATH is the marker path)."
        )
    }

    /**
     * Persists the "migration is not required" marker, tolerating a benign race with another replica
     * doing the same thing on first startup: both may observe zero deployments and both call this method
     * concurrently. ZooKeeper allows only one of the concurrent writes to succeed (the other typically
     * fails with [KeeperException.NodeExistsException] or [KeeperException.BadVersionException]
     * depending on exact timing). If our own write lost that race, re-reading the marker will find the
     * value the winner wrote, and that is just as good as writing it ourselves - so we treat it as
     * success. Only if the marker is still missing after a failed write do we propagate the original
     * error, since that indicates a real ZooKeeper problem rather than a concurrent winner.
     */
    private fun persistMigrationNotRequiredMarker() {
        val marker = MigrationNotRequiredMarker(
            createdAt = Instant.now(),
            reason = "No Camunda deployments found on the first startup with MongoDB disabled. " +
                "Clean installation is assumed, migration is not required."
        )
        try {
            zooKeeper.setValue(MIGRATION_NOT_REQUIRED_MARKER_PATH, marker, persistent = true)
        } catch (e: KeeperException) {
            val markerWrittenConcurrently = zooKeeper.getValue(
                MIGRATION_NOT_REQUIRED_MARKER_PATH,
                MigrationNotRequiredMarker::class.java
            )
            if (markerWrittenConcurrently != null) {
                log.info(e) {
                    "===== Failed to write the migration-not-required marker to ZooKeeper, but it is " +
                        "already present - assuming another replica won the race on first startup ====="
                }
            } else {
                throw e
            }
        }
    }
}
