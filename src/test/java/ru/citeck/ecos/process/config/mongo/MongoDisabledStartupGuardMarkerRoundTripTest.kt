package ru.citeck.ecos.process.config.mongo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.zookeeper.test.EcosZooKeeperTest
import java.time.Instant

/**
 * The [MongoDisabledStartupGuard] "clean installation" fix relies entirely on the assumption that a
 * [MongoDisabledStartupGuard.MigrationNotRequiredMarker] written to ZooKeeper via `setValue` can later
 * be read back via `getValue`. This is not a safe assumption to leave unverified: `EcosZooKeeper`'s
 * `readNodeValue` catches deserialization errors internally and returns `null` instead of throwing (see
 * `EcosZooKeeper.readNodeValue` in `ecos-zookeeper`), so a broken round-trip (e.g. a field rename, or a
 * regression in how `Instant` is (de)serialized) would not surface as an exception anywhere - it would
 * just make `getValue` silently return `null`, which is indistinguishable from "no marker was ever
 * written". On an installation that depends on the marker to start (deployments now exist, so the
 * deployment-count branch would refuse to start), that failure mode is a silent, permanent crashloop:
 * the marker node exists in ZooKeeper, but the guard can never read it back.
 *
 * A test using a mocked [ru.citeck.ecos.zookeeper.EcosZooKeeper] cannot catch this class of bug: a mock
 * only ever echoes back whatever it is told to return. This test instead runs against a real ZooKeeper
 * server (via `ecos-zookeeper-test`, backed by testcontainers) to exercise the actual serialization
 * round-trip that production relies on.
 */
class MongoDisabledStartupGuardMarkerRoundTripTest {

    @Test
    fun `marker written to a real zookeeper must be readable back with all fields intact`() {
        val zooKeeper = EcosZooKeeperTest.getZooKeeper(this::class)

        val marker = MongoDisabledStartupGuard.MigrationNotRequiredMarker(
            createdAt = Instant.parse("2026-01-01T12:34:56.789Z"),
            reason = "round-trip test marker"
        )

        zooKeeper.setValue(
            MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH,
            marker,
            persistent = true
        )

        val readBack = zooKeeper.getValue(
            MongoDisabledStartupGuard.MIGRATION_NOT_REQUIRED_MARKER_PATH,
            MongoDisabledStartupGuard.MigrationNotRequiredMarker::class.java
        )

        assertThat(readBack).isNotNull
        assertThat(readBack!!.createdAt).isEqualTo(marker.createdAt)
        assertThat(readBack.reason).isEqualTo(marker.reason)
        assertThat(readBack).isEqualTo(marker)
    }
}
