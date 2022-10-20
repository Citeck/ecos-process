package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

private const val DEPLOYED_TAG = "deployed"
private const val EDIT_BPMN_PROC_LINK = "bpmn-editor?recordRef=%s"

@Component
class BpmnProcDefVersionRecords(
    private val procDefService: ProcDefService,
    private val bpmnProcDefRecords: BpmnProcDefRecords,
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDtoDao<BpmnProcDefRecords.BpmnMutateRecord> {

    companion object {
        const val ID = "${BpmnProcDefRecords.SOURCE_ID}-version"
    }

    private val versionRef = ThreadLocal<EntityRef>()

    override fun getId(): String {
        return ID
    }

    override fun getRecToMutate(recordId: String): BpmnProcDefRecords.BpmnMutateRecord {
        val procDefRev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, UUID.fromString(recordId))
            ?: error("Process definition not found for version: $recordId")

        if (procDefRev.procDefId.isNullOrBlank()) {
            error("Process definition id is blank for version: $recordId")
        }

        versionRef.set(procDefRev.getRef())

        return bpmnProcDefRecords.getRecToMutate(procDefRev.procDefId.toString())
    }

    override fun saveMutatedRec(record: BpmnProcDefRecords.BpmnMutateRecord): String {
        record.createdFromVersion = versionRef.get()
        versionRef.remove()

        return bpmnProcDefRecords.saveMutatedRec(record)
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val record = recsQuery.getQuery(VersionQuery::class.java).record
        val procDefRef = ProcDefRef.create(BPMN_PROC_TYPE, record.getLocalId())

        val versions = procDefService.getProcessDefRevs(procDefRef).map {
            it.toVersionRecord()
        }

        val result = RecsQueryRes<VersionRecord>()
        result.setRecords(versions)

        return result
    }

    override fun getRecordsAtts(recordsId: List<String>): List<Any> {
        return procDefService.getProcessDefRevs(recordsId.map { UUID.fromString(it) }).map {
            it.toVersionRecord()
        }
    }


    private fun ProcDefRevDto.toVersionRecord(): VersionRecord {

        val externalVersion = version.toDouble().inc()

        return VersionRecord(
            id = id.toString(),
            version = externalVersion,
            deploymentId = deploymentId ?: "",
            procDefId = procDefId,
            modified = created,
            downloadUrl = "/gateway/${AppName.EPROC}/api/proc-def/version/data?ref=${getRef()}",
            fileName = "${procDefId}_v$externalVersion.bpmn.xml",
            name = MLText.EMPTY,
            modifier = if (createdBy.isNullOrBlank()) {
                RecordRef.EMPTY
            } else {
                RecordRef.create(AppName.EMODEL, "person", createdBy)
            },
            data = data,
            format = format,
            tags = if (deploymentId.isNullOrBlank()) {
                emptyList()
            } else {
                listOf(DEPLOYED_TAG)
            }
        )
    }

    private inner class VersionRecord(
        val id: String,
        val version: Double,
        val deploymentId: String = "",
        val modified: Instant,
        val downloadUrl: String = "",
        val fileName: String = "",
        val comment: String = "",
        val name: MLText,
        val modifier: EntityRef,
        val data: ByteArray,
        val format: String = "",
        val procDefId: String,
        val tags: List<String> = emptyList(),
        val editLink: String = EDIT_BPMN_PROC_LINK.format(RecordRef.create(AppName.EPROC, ID, id)),
    ) {

        @get:AttName(".disp")
        val disp: String
            get() = let {
                var defName = procDefService.getProcessDefById(
                    ProcDefRef.create(BPMN_PROC_TYPE, procDefId)
                )?.name?.getClosest(I18nContext.getLocale()) ?: ""

                if (defName.isBlank()) {
                    defName = procDefId
                }

                return "$defName $version"
            }

        @get:AttName("definition")
        val definition: String
            get() = String(data, Charsets.UTF_8)
    }

}


private fun ProcDefRevDto.getRef(): EntityRef {
    return RecordRef.create(AppName.EPROC, BpmnProcDefVersionRecords.ID, id.toString())
}

private data class VersionQuery(
    val record: EntityRef
)
