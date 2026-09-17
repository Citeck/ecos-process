package ru.citeck.ecos.process.domain.proctask.candidatessync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val ATT_DOCUMENT_REF = "documentRef"

/**
 * Entry point for external systems: after a document role's membership changes, they call this
 * action (records mutate) with the document ref, and it re-syncs candidateUsers/candidateGroups
 * of that document's active tasks with the current role composition.
 */
@Component
class ProcTaskCandidatesSyncRecords(
    private val syncService: ProcTaskCandidatesSyncService
) : AbstractRecordsDao(),
    RecordMutateDao {

    companion object {
        const val ID = "proctask-candidates-sync"
        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun mutate(record: LocalRecordAtts): String {
        val documentRef = EntityRef.valueOf(record.getAtt(ATT_DOCUMENT_REF).asText())
        require(documentRef != EntityRef.EMPTY) { "documentRef is mandatory for candidates sync" }

        log.debug { "Sync task candidates for document $documentRef" }

        syncService.syncCandidates(documentRef)

        return documentRef.getLocalId()
    }
}
