package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
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
private const val DRAFT_TAG = "draft"
private const val EDIT_BPMN_PROC_LINK = "bpmn-editor?recordRef=%s"

// TODO: permissions and tests
@Component
class BpmnProcessDefVersionRecords(
    private val procDefService: ProcDefService,
    private val bpmnProcessDefRecords: BpmnProcessDefRecords
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDtoDao<BpmnProcessDefRecords.BpmnMutateRecord> {

    companion object {
        const val ID = "$BPMN_PROCESS_DEF_RECORDS_SOURCE_ID-version"
    }

    private val versionRef = ThreadLocal<EntityRef>()

    override fun getId(): String {
        return ID
    }

    override fun getRecToMutate(recordId: String): BpmnProcessDefRecords.BpmnMutateRecord {
        val procDefRev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, UUID.fromString(recordId))
            ?: error("Process definition not found for version: $recordId")

        if (procDefRev.procDefId.isBlank()) {
            error("Process definition id is blank for version: $recordId")
        }

        versionRef.set(procDefRev.getRef())

        return bpmnProcessDefRecords.getRecToMutate(procDefRev.procDefId)
    }

    override fun saveMutatedRec(record: BpmnProcessDefRecords.BpmnMutateRecord): String {
        record.createdFromVersion = versionRef.get()
        versionRef.remove()

        return bpmnProcessDefRecords.saveMutatedRec(record)
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val query = recsQuery.getQuery(VersionQuery::class.java)
        val procDefRef = ProcDefRef.create(BPMN_PROC_TYPE, query.record.getLocalId())

        var versions = procDefService.getProcessDefRevs(procDefRef).map {
            it.toVersionRecord()
        }

        if (query.onlyDeployed) {
            versions = versions.filter { it.deploymentId.isNotBlank() }
        }

        val result = RecsQueryRes<VersionRecord>()
        result.setRecords(versions)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<Any> {
        return procDefService.getProcessDefRevs(recordIds.map { UUID.fromString(it) })
            .map {
                it.toVersionRecord()
            }
            .sortByIds(recordIds)
    }

    private fun ProcDefRevDto.toVersionRecord(): VersionRecord {

        val externalVersion = version.toDouble().inc()

        return VersionRecord(
            id = id.toString(),
            version = externalVersion,
            deploymentId = deploymentId ?: "",
            dataState = dataState,
            procDefId = procDefId,
            modified = created,
            downloadUrl = "/gateway/${AppName.EPROC}/api/proc-def/version/data?ref=${getRef()}",
            fileName = "${procDefId}_v$externalVersion.bpmn.xml",
            name = MLText.EMPTY,
            comment = comment,
            modifier = if (createdBy.isNullOrBlank()) {
                RecordRef.EMPTY
            } else {
                RecordRef.create(AppName.EMODEL, "person", createdBy)
            },
            format = format,
            tags = let {
                val tags = mutableListOf<String>()

                if (!deploymentId.isNullOrBlank()) {
                    tags.add(DEPLOYED_TAG)
                }

                if (dataState == ProcDefRevDataState.RAW) {
                    tags.add(DRAFT_TAG)
                }

                tags
            },
            dto = this
        )
    }

    private inner class VersionRecord(
        val id: String,
        val version: Double,
        val deploymentId: String = "",
        val dataState: ProcDefRevDataState,
        val modified: Instant,
        val downloadUrl: String = "",
        val fileName: String = "",
        val comment: String = "",
        val name: MLText,
        val modifier: EntityRef,
        val format: String = "",
        val procDefId: String,
        val tags: List<String> = emptyList(),
        val editLink: String = EDIT_BPMN_PROC_LINK.format(RecordRef.create(AppName.EPROC, ID, id)),

        private val dto: ProcDefRevDto
    ) : IdentifiableRecord {

        @get:AttName(".disp")
        val disp: MLText
            get() = let {
                var defName = procDefService.getProcessDefById(
                    ProcDefRef.create(BPMN_PROC_TYPE, procDefId)
                )?.name?.getClosest(I18nContext.getLocale()) ?: ""

                if (defName.isBlank()) {
                    defName = procDefId
                }

                val disp = "$defName $version"

                if (dataState == ProcDefRevDataState.RAW) {
                    return MLText(
                        I18nContext.ENGLISH to "$disp (draft)",
                        I18nContext.RUSSIAN to "$disp (черновик)"
                    )
                }

                return MLText(disp)
            }

        @get:AttName(RecordConstants.ATT_MODIFIED)
        val attModified: Instant
            get() = modified

        @get:AttName(RecordConstants.ATT_MODIFIER)
        val attModifier: EntityRef
            get() = modifier

        @get:AttName("definition")
        val definition: String
            get() = String(data, Charsets.UTF_8)

        @get:AttName("processDefRef")
        val processDefRef: EntityRef
            get() = EntityRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, procDefId)

        /**
         *  Lazy load data to avoid memory leaks. See [ProcDefRevDto.data]
         */
        @get:AttName("data")
        val data: ByteArray
            get() = dto.data

        override fun getIdentificator(): String {
            return id
        }
    }
}

private fun ProcDefRevDto.getRef(): EntityRef {
    return RecordRef.create(AppName.EPROC, BpmnProcessDefVersionRecords.ID, id.toString())
}

data class VersionQuery(
    val record: EntityRef,
    val onlyDeployed: Boolean = false
)
