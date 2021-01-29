package ru.citeck.ecos.process.domain.cmmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.op.delete.dao.RecordDeleteDao
import ru.citeck.ecos.records3.record.op.delete.dto.DelStatus
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import java.nio.charset.StandardCharsets

@Component
class CmmnProcDefRecords(
    val procDefService: ProcDefService
) : RecordsQueryDao, RecordAttsDao, RecordDeleteDao,
    RecordMutateDtoDao<CmmnProcDefRecords.CmmnMutateRecord> {

    companion object {
        private const val SOURCE_ID = "cmmn-def"
        private const val PROC_TYPE = "cmmn"
    }

    override fun queryRecords(query: RecordsQuery): Any? {

        if (query.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = Predicates.and(
            query.getQuery(Predicate::class.java),
            Predicates.eq("procType", PROC_TYPE)
        )

        val result = procDefService.findAll(
            predicate,
            query.page.maxItems,
            query.page.skipCount
        ).map { CmmnProcDefRecord(it) }

        val res = RecsQueryRes(result)
        res.setTotalCount(procDefService.getCount(predicate))
        res.setHasMore(res.getTotalCount() > query.page.maxItems + query.page.skipCount)

        return res
    }

    override fun getRecordAtts(record: String): Any? {

        val ref = ProcDefRef.create(PROC_TYPE, record)
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let { CmmnProcDefRecord(ProcDefDto(
            it.id,
            it.name,
            it.procType,
            it.format,
            it.revisionId,
            it.ecosTypeRef,
            it.alfType,
            it.enabled
        )) } ?: EmptyAttValue.INSTANCE
    }

    override fun getRecToMutate(recordId: String): CmmnMutateRecord {
        return if (recordId.isBlank()) {
            CmmnMutateRecord("", "", MLText(), RecordRef.EMPTY, null, true)
        } else {
            val procDef = procDefService.getProcessDefById(ProcDefRef.create(PROC_TYPE, recordId))
                ?: error("Process definition is not found: $recordId")
            CmmnMutateRecord(
                recordId,
                recordId,
                procDef.name ?: MLText(),
                procDef.ecosTypeRef,
                null,
                procDef.enabled
            )
        }
    }

    override fun saveMutatedRec(record: CmmnMutateRecord): String {

        if (record.processDefId.isBlank()) {
            error("processDefId is missing")
        }

        val newRef = ProcDefRef.create(PROC_TYPE, record.processDefId)

        val currentProc = procDefService.getProcessDefById(newRef)

        if ((record.id != record.processDefId) && currentProc != null) {
            error("Process definition with id " + newRef.id + " already exists")
        }

        val newDefinition = record.definition ?: ""

        if (currentProc == null) {

            val newProcDef = NewProcessDefDto()

            newProcDef.id = record.processDefId
            val definition = if (newDefinition.isNotBlank()) {
                val newDef = CmmnIO.import(newDefinition)
                CmmnIO.export(newDef)
                newDef
            } else {
                CmmnIO.generateDefaultDef(record.processDefId, record.name, record.ecosType)
            }
            newProcDef.name = definition.name
            newProcDef.data = Json.mapper.toBytes(definition)
            newProcDef.ecosTypeRef = definition.ecosType
            newProcDef.format = "ecos-cmmn"
            newProcDef.procType = PROC_TYPE

            procDefService.uploadProcDef(newProcDef)

        } else {

            if (newDefinition.isNotBlank()) {

                val cmmnProcDef = CmmnIO.import(newDefinition)
                CmmnIO.export(cmmnProcDef)

                currentProc.data = Json.mapper.toBytes(cmmnProcDef)
                currentProc.ecosTypeRef = cmmnProcDef.ecosType
                currentProc.name = cmmnProcDef.name

            } else {

                currentProc.ecosTypeRef = record.ecosType
                currentProc.name = record.name
            }

            currentProc.enabled = record.enabled

            procDefService.uploadNewRev(currentProc)
        }

        return record.processDefId
    }

    override fun delete(recordId: String): DelStatus {
        procDefService.delete(ProcDefRef.create(PROC_TYPE, recordId))
        return DelStatus.OK
    }

    override fun getId() = SOURCE_ID

    inner class CmmnProcDefRecord(
        private val procDef: ProcDefDto
    ) {

        fun getId(): String {
            return procDef.id
        }

        fun getName(): MLText {
            return procDef.name ?: MLText()
        }

        fun getFormat(): String {
            return procDef.format
        }

        @AttName("?disp")
        fun getDisplayName(): String {
            return MLText.getClosestValue(getName(), RequestContext.getLocale())
        }

        fun getEnabled(): Boolean {
            return procDef.enabled
        }

        fun getData(): ByteArray? {
            return getDefinition()?.toByteArray(StandardCharsets.UTF_8)
        }

        fun getArtifactId() = procDef.id

        fun getModuleId() = procDef.id

        fun getProcessDefId() = procDef.id

        fun getDefinition(): String? {
            val rev = procDefService.getProcessDefRev(PROC_TYPE, procDef.revisionId) ?: return null
            if (rev.format == "ecos-cmmn") {
                return Json.mapper.read(rev.data, CmmnProcDef::class.java)?.let { CmmnIO.exportToString(it) }
            }
            return String(rev.data, StandardCharsets.UTF_8)
        }

        @AttName("?json")
        fun getJson(): CmmnProcDef? {
            val rev = procDefService.getProcessDefRev(PROC_TYPE, procDef.revisionId) ?: return null
            if (rev.format != "ecos-cmmn") {
                error("Json representation allowed only for ecos-cmmn processes")
            }
            return Json.mapper.read(rev.data, CmmnProcDef::class.java)
        }

        @AttName("_type")
        fun getType(): RecordRef {
            return RecordRef.create("emodel", "type", "cmmn-process-def")
        }
    }

    class CmmnMutateRecord(
        var id: String,
        var processDefId: String,
        var name: MLText,
        var ecosType: RecordRef,
        var definition: String? = null,
        var enabled: Boolean
    )
}
