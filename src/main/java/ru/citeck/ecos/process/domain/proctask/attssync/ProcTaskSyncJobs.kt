package ru.citeck.ecos.process.domain.proctask.attssync

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.data.sql.records.DbRecordsDao
import ru.citeck.ecos.data.sql.repo.entity.DbEntity
import ru.citeck.ecos.process.domain.proctask.config.TASK_SYNC_JOB_SOURCE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.lock.EcosAppLockService
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Durable checkpoint, independent of the Rabbit delivery and Camunda task transactions. */
@Component
class ProcTaskSyncJobs(
    private val records: RecordsService,
    private val locks: EcosAppLockService
) {
    fun register(request: TaskAttsSyncQueueMessage) {
        // Old producers provide no stable identity: equal payloads can be independent requests.
        val requestId = request.requestId.ifBlank { "legacy:${UUID.randomUUID()}" }
        // Lock implementations cache keys: keep registration coordination bounded and separate from task work.
        locks.doInSync("task-atts-sync-register", Duration.ofSeconds(10)) {
            AuthContext.runAsSystem {
                TxnContext.doInNewTxn {
                    val existing = records.query(
                        RecordsQuery.create {
                            withSourceId(TASK_SYNC_JOB_SOURCE)
                            withQuery(Predicates.eq("requestId", requestId))
                            withMaxItems(1)
                        }
                    ).getRecords()
                    if (existing.isEmpty()) {
                        records.create(
                            TASK_SYNC_JOB_SOURCE,
                            mapOf(
                                "requestId" to requestId,
                                "documentType" to request.documentTypeRef.toString(),
                                "syncMode" to request.syncMode.name,
                                "lastTaskId" to "",
                                "processed" to 0,
                                "completed" to false,
                                "createdBefore" to Instant.now(),
                                "nextAttempt" to Instant.EPOCH
                            )
                        )
                    }
                }
            }
        }
    }

    fun pending(): List<TaskAttsSyncJobSnapshot> = AuthContext.runAsSystem {
        TxnContext.doInNewTxn {
            records.query(
                RecordsQuery.create {
                    withSourceId(TASK_SYNC_JOB_SOURCE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("completed", false),
                            Predicates.le("nextAttempt", Instant.now())
                        )
                    )
                    withSortBy(SortBy("nextAttempt", true))
                    withMaxItems(1)
                }
            ).getRecords().mapNotNull { ref ->
                val entity = dataService.findByExtId(ref.getLocalId()) ?: return@mapNotNull null
                val job = Json.mapper.convert(entity.attributes, TaskAttsSyncJob::class.java)
                    ?: error("Invalid task sync job: $ref")
                if (job.completed) null else TaskAttsSyncJobSnapshot(job.copy(ref = ref), entity)
            }
        }
    }

    // Save the pre-page version, not a newly loaded Records entity: stale owners must not overwrite progress.
    fun save(job: TaskAttsSyncJob, snapshot: DbEntity, error: Boolean = false) = AuthContext.runAsSystem {
        TxnContext.doInNewTxn {
            val updated = snapshot.copy()
            updated.attributes["lastTaskId"] = job.lastTaskId
            updated.attributes["processed"] = job.processed
            updated.attributes["completed"] = job.completed
            updated.attributes["nextAttempt"] = if (error) Instant.now().plusSeconds(30) else Instant.now()
            dataService.save(updated)
        }
    }

    private val dataService
        get() = checkNotNull(records.getRecordsDao(TASK_SYNC_JOB_SOURCE, DbRecordsDao::class.java))
            .getRecordsDaoCtx().dataService
}

data class TaskAttsSyncJobSnapshot(val job: TaskAttsSyncJob, val entity: DbEntity)

data class TaskAttsSyncJob(
    @AttName("id") val ref: EntityRef = EntityRef.EMPTY,
    val requestId: String = "",
    val documentType: EntityRef = EntityRef.EMPTY,
    val syncMode: SyncMode = SyncMode.FULL,
    val lastTaskId: String = "",
    val processed: Long = 0,
    val completed: Boolean = false,
    val createdBefore: Instant = Instant.EPOCH
)
