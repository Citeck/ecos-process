package ru.citeck.ecos.process.domain.proctask.attssync

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.config.TASK_SYNC_JOB_SOURCE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.lock.EcosAppLockService
import java.nio.charset.StandardCharsets
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
        locks.doInSync("task-atts-sync-register:${UUID.nameUUIDFromBytes(requestId.toByteArray(StandardCharsets.UTF_8))}", Duration.ofSeconds(10)) {
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

    fun pending(): List<TaskAttsSyncJob> = AuthContext.runAsSystem {
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
                },
                TaskAttsSyncJob::class.java
            ).getRecords()
        }
    }

    fun save(job: TaskAttsSyncJob, error: Boolean = false) = AuthContext.runAsSystem {
        TxnContext.doInNewTxn {
            records.mutate(
                RecordAtts(job.ref).apply {
                    setAtt("lastTaskId", job.lastTaskId)
                    setAtt("processed", job.processed)
                    setAtt("completed", job.completed)
                    setAtt("nextAttempt", if (error) Instant.now().plusSeconds(30) else Instant.now())
                }
            )
        }
    }
}

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
