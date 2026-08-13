package ru.citeck.ecos.process.config.mongo

import org.apache.zookeeper.KeeperException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.repository.DeploymentQuery
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.citeck.ecos.process.common.patch.MongoToEcosDataMigrationConfig
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosLocalPatch
import ru.citeck.ecos.webapp.lib.patch.local.EcosLocalPatchRunner
import ru.citeck.ecos.zookeeper.EcosZooKeeper
import java.time.Instant

class MongoDisabledStartupGuardTest {

    private val webAppApi = mock(EcosWebAppApi::class.java)
    private val patchRunner = mock(EcosLocalPatchRunner::class.java)
    private val repositoryService = mock(RepositoryService::class.java)
    private val zooKeeper = mock(EcosZooKeeper::class.java)

    private val guard = MongoDisabledStartupGuard(webAppApi, patchRunner, repositoryService, zooKeeper)

    private fun givenMigrationExecuted(executed: Boolean) {
        whenever(
            patchRunner.isLocalPatchExecuted(
                MongoToEcosDataMigrationConfig.MongoToEcosDataMigration::class.java
            )
        ).thenReturn(executed)
    }

    private fun givenDeploymentsCount(count: Long) {
        val query = mock(DeploymentQuery::class.java)
        whenever(query.count()).thenReturn(count)
        whenever(repositoryService.createDeploymentQuery()).thenReturn(query)
    }

    private fun givenMarkerInZk(marker: MongoDisabledStartupGuard.MigrationNotRequiredMarker?) {
        whenever(
            zooKeeper.getValue(
                eq(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH),
                eq(MongoDisabledStartupGuard.MigrationNotRequiredMarker::class.java)
            )
        ).thenReturn(marker)
    }

    @Test
    fun `should pass when migration patch is executed`() {
        givenMigrationExecuted(true)

        assertThatCode { guard.checkMigrationCompleted() }.doesNotThrowAnyException()

        verify(repositoryService, never()).createDeploymentQuery()
        verify(zooKeeper, never()).getValue(any(), eq(MongoDisabledStartupGuard.MigrationNotRequiredMarker::class.java))
        verify(zooKeeper, never()).setValue(any(), any(), any())
    }

    @Test
    fun `should pass and skip deployment check when marker already exists in zookeeper`() {
        givenMigrationExecuted(false)
        givenMarkerInZk(
            MongoDisabledStartupGuard.MigrationNotRequiredMarker(
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                reason = "test marker"
            )
        )

        assertThatCode { guard.checkMigrationCompleted() }.doesNotThrowAnyException()

        verify(repositoryService, never()).createDeploymentQuery()
        verify(zooKeeper, never()).setValue(any(), any(), any())
    }

    @Test
    fun `should pass on clean installation without deployments and persist marker in zookeeper`() {
        givenMigrationExecuted(false)
        givenMarkerInZk(null)
        givenDeploymentsCount(0)

        assertThatCode { guard.checkMigrationCompleted() }.doesNotThrowAnyException()

        verify(zooKeeper).setValue(
            eq(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH),
            any<MongoDisabledStartupGuard.MigrationNotRequiredMarker>(),
            eq(true)
        )
    }

    @Test
    fun `marker path must remain stable because it is already persisted on live installations`() {
        // A literal, not a reference to the constant: this test must fail if someone renames or
        // moves MIGRATION_NOT_REQUIRED_MARKER_PATH, because doing so would silently invalidate every
        // marker already persisted in ZooKeeper on installations that rely on it to start.
        assertThat(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH)
            .isEqualTo("/eproc/mongo/migration-not-required")
    }

    @Test
    fun `should tolerate concurrent marker write by another replica and pass when marker appears after failed write`() {
        givenMigrationExecuted(false)
        givenDeploymentsCount(0)

        val markerWrittenByOtherReplica = MongoDisabledStartupGuard.MigrationNotRequiredMarker(
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            reason = "written by another replica"
        )
        // First read (before the deployment-count check) finds nothing; the retry read performed
        // after our own write fails finds the marker the other replica managed to persist first.
        whenever(
            zooKeeper.getValue(
                eq(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH),
                eq(MongoDisabledStartupGuard.MigrationNotRequiredMarker::class.java)
            )
        ).thenReturn(null, markerWrittenByOtherReplica)

        // KeeperException is a checked exception that EcosZooKeeper.setValue does not declare, so
        // Mockito's thenThrow(...) rejects it as "invalid for this method". thenAnswer bypasses that
        // compile-time-only check and throws it for real, exactly like the genuine client would.
        whenever(
            zooKeeper.setValue(
                eq(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH),
                any<MongoDisabledStartupGuard.MigrationNotRequiredMarker>(),
                eq(true)
            )
        ).thenAnswer { throw KeeperException.create(KeeperException.Code.NODEEXISTS) }

        assertThatCode { guard.checkMigrationCompleted() }.doesNotThrowAnyException()
    }

    @Test
    fun `should propagate zookeeper write failure when marker is still absent after the failed write`() {
        givenMigrationExecuted(false)
        givenMarkerInZk(null)
        givenDeploymentsCount(0)

        val writeFailure = KeeperException.create(KeeperException.Code.CONNECTIONLOSS)
        whenever(
            zooKeeper.setValue(
                eq(MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH),
                any<MongoDisabledStartupGuard.MigrationNotRequiredMarker>(),
                eq(true)
            )
        ).thenAnswer { throw writeFailure }

        assertThatThrownBy { guard.checkMigrationCompleted() }
            .isSameAs(writeFailure)
    }

    @Test
    fun `should fail when migration is not executed and deployments exist and must not persist marker`() {
        givenMigrationExecuted(false)
        givenMarkerInZk(null)
        givenDeploymentsCount(5)

        assertThatThrownBy { guard.checkMigrationCompleted() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("mongo-to-ecos-data-migration2")
            .hasMessageContaining("ecos-process.mongo.enabled")

        verify(zooKeeper, never()).setValue(any(), any(), any())
    }

    @Test
    fun `should fail fast when mongo repo is kept as primary storage while mongo is disabled`() {
        val guardWithConflictingConfig = MongoDisabledStartupGuard(
            webAppApi,
            patchRunner,
            repositoryService,
            zooKeeper,
            mongoRepoEnabledByProperty = true
        )

        assertThatThrownBy { guardWithConflictingConfig.checkMigrationCompleted() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("ecos-process.repo.mongo.enabled")
            .hasMessageContaining("ecos-process.mongo.enabled")

        verify(patchRunner, never()).isLocalPatchExecuted(any())
        verify(zooKeeper, never()).getValue(any(), eq(MongoDisabledStartupGuard.MigrationNotRequiredMarker::class.java))
        verify(repositoryService, never()).createDeploymentQuery()
    }

    @Test
    fun `should register check before app ready with order 0`() {
        guard.init()

        verify(webAppApi).doBeforeAppReady(eq(0f), any())
    }

    /**
     * The guard runs from doBeforeAppReady, and the migration patch must run strictly after it,
     * otherwise the patch would mark itself as executed on a system which was not migrated yet
     * and the guard would let such a system start with all its data invisible.
     *
     * That ordering is guaranteed by afterStart = true: such patches are executed from the
     * doWhenAppReady queue, which is drained after the whole doBeforeAppReady queue.
     * The default value of afterStart is false, so dropping it here would silently break the guard.
     */
    @Test
    fun `migration patch must be declared as afterStart to run later than the guard`() {
        val patchAnnotation = MongoToEcosDataMigrationConfig.MongoToEcosDataMigration::class.java
            .getAnnotation(EcosLocalPatch::class.java)

        assertThat(patchAnnotation).isNotNull
        assertThat(patchAnnotation.afterStart)
            .`as`("MongoToEcosDataMigration must stay afterStart, otherwise MongoDisabledStartupGuard may run after it")
            .isTrue()
    }
}
