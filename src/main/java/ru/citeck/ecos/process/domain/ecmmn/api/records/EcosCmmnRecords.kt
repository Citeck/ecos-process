package ru.citeck.ecos.process.domain.ecmmn.api.records

import org.springframework.stereotype.Component
import org.springframework.util.MimeTypeUtils
import ru.citeck.ecos.apps.artifact.ArtifactRef
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.ecmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.ecmmn.model.CmmnProcDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.op.atts.dao.RecordAttsDao
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName
import ru.citeck.ecos.records3.record.op.atts.service.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.op.mutate.dao.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.op.query.dao.RecordsQueryDao
import ru.citeck.ecos.records3.record.op.query.dto.RecsQueryRes
import ru.citeck.ecos.records3.record.op.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.request.RequestContext
import java.nio.charset.StandardCharsets

@Component
class EcosCmmnRecords(
    val procDefService: ProcDefService
) : RecordsQueryDao, RecordAttsDao, RecordMutateDtoDao<EcosCmmnRecords.EcmmnMutateRecord> {

    companion object {
        private const val PROC_TYPE = "ecmmn"
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
        ).map { EcmmnProcDefRecord(it) }

        val res = RecsQueryRes(result)
        res.setTotalCount(procDefService.getCount(predicate))
        res.setHasMore(res.getTotalCount() > query.page.maxItems + query.page.skipCount)

        return res
    }

    override fun getRecordAtts(record: String): Any? {

        val ref = ArtifactRef.create(PROC_TYPE, record)
        val currentProc = procDefService.getProcessDefById(ref).orElse(null)

        return currentProc?.let { EcmmnProcDefRecord(ProcDefDto(
            it.id,
            it.name,
            it.procType,
            it.revisionId,
            it.ecosTypeRef,
            it.alfType,
            it.enabled
        )) } ?: EmptyAttValue.INSTANCE
    }

    override fun getRecToMutate(recordId: String): EcmmnMutateRecord {
        return if (recordId.isBlank()) {
            EcmmnMutateRecord("", MLText(), RecordRef.EMPTY, null, true)
        } else {
            val procDef = procDefService.getProcessDefById(ArtifactRef.create(PROC_TYPE, recordId)).orElse(null)
                ?: error("Process definition is not found: $recordId")
            EcmmnMutateRecord(
                recordId,
                procDef.name ?: MLText(),
                procDef.ecosTypeRef,
                null,
                procDef.enabled
            )
        }
    }

    override fun saveMutatedRec(record: EcmmnMutateRecord): String {

        if (record.artifactId.isBlank()) {
            error("artifactId is missing")
        }

        val ref = ArtifactRef.create(PROC_TYPE, record.artifactId)
        val currentProc = procDefService.getProcessDefById(ref).orElse(null)

        val newDefinition = record.definition ?: ""

        if (currentProc == null) {

            val newProcDef = NewProcessDefDto()

            newProcDef.id = record.artifactId
            val definition = if (newDefinition.isNotBlank()) {
                val newDef = CmmnIO.import(newDefinition)
                CmmnIO.export(newDef)
                newDef
            } else {
                CmmnIO.generateDefaultDef(record.artifactId, record.name, record.ecosType)
            }
            newProcDef.name = definition.name
            newProcDef.data = Json.mapper.toBytes(definition)
            newProcDef.ecosTypeRef = definition.ecosType
            newProcDef.format = MimeTypeUtils.APPLICATION_JSON_VALUE
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

        return record.artifactId
    }

    override fun getId() = PROC_TYPE

    inner class EcmmnProcDefRecord(
        val procDef: ProcDefDto
    ) {

        fun getId(): String {
            return procDef.id
        }

        fun getName(): MLText {
            return procDef.name ?: MLText()
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

        fun getDefinition(): String? {

            val rev = procDefService.getProcessDefRev(PROC_TYPE, procDef.revisionId)
                .orElse(null) ?: return null
            val procDef = Json.mapper.read(rev.data, CmmnProcDef::class.java) ?: return null

            return CmmnIO.exportToString(procDef)
        }

        @AttName("_type")
        fun getType(): RecordRef {
            return RecordRef.create("emodel", "type", "ecmmn-process-def")
        }
    }

    class EcmmnMutateRecord(
        var artifactId: String,
        var name: MLText,
        var ecosType: RecordRef,
        var definition: String? = null,
        var enabled: Boolean
    ) {
        fun getId() = artifactId
    }
}
