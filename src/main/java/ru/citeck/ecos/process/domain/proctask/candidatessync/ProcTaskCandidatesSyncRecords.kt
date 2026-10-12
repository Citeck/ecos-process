package ru.citeck.ecos.process.domain.proctask.candidatessync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateWithAnyResDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val ATT_DOCUMENT_REF = "documentRef"

/**
 * Entry point for external systems: after a document role's membership changes, they call this
 * action (records mutate) with the document ref, and it re-syncs candidateUsers/candidateGroups
 * of that document's active tasks with the current role composition.
 *
 * The action must be called after the transaction that changes the role membership is committed
 * (e.g. `TxnContext.doAfterCommit`): roles are re-resolved by reading the document, so a call made
 * before the commit sees the old role composition and changes nothing.
 *
 * Returns [ProcTaskCandidatesSyncResult] - ids of updated, unassigned and failed tasks, e.g.:
 * `recordsService.mutateAndGetAtts(ref, atts, listOf("updatedTaskIds[]", "unassignedTaskIds[]", "failedTaskIds[]"))`
 */
@Component
class ProcTaskCandidatesSyncRecords(
    private val syncService: ProcTaskCandidatesSyncService
) : AbstractRecordsDao(),
    RecordMutateWithAnyResDao {

    companion object {
        const val ID = "proctask-candidates-sync"
        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun mutateForAnyRes(record: LocalRecordAtts): ProcTaskCandidatesSyncResult {
        if (!AuthContext.isRunAsSystemOrAdmin()) {
            error("Permissions denied. Task sync is available for System or Admin roles only")
        }
        val documentRef = EntityRef.valueOf(record.getAtt(ATT_DOCUMENT_REF).asText())
        require(documentRef != EntityRef.EMPTY) { "documentRef is mandatory for candidates sync" }

        log.debug { "Sync task candidates for document $documentRef" }

        return syncService.syncCandidates(documentRef)
    }
}
