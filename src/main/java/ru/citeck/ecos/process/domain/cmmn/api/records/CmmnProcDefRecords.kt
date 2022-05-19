package ru.citeck.ecos.process.domain.cmmn.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.process.domain.cmmn.io.CmmnFormat
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.cmmn.io.CmmnProcDefImporter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.delete.RecordDeleteDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CmmnProcDefRecords(
    val procDefService: ProcDefService,
    val cmmnProcDefImporter: CmmnProcDefImporter
) : RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<CmmnProcDefRecords.CmmnMutateRecord> {

    companion object {
        private const val SOURCE_ID = "cmmn-def"
        private const val PROC_TYPE = "cmmn"

        private val log = KotlinLogging.logger {}
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

        return currentProc?.let {
            CmmnProcDefRecord(
                ProcDefDto(
                    it.id,
                    it.name,
                    it.procType,
                    it.format,
                    it.revisionId,
                    it.ecosTypeRef,
                    it.alfType,
                    it.formRef,
                    it.enabled
                )
            )
        } ?: EmptyAttValue.INSTANCE
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

        val newDefinition = record.definition ?: ""
        var newProcDefDto: NewProcessDefDto? = null

        if (newDefinition.isNotBlank()) {

            newProcDefDto = cmmnProcDefImporter.getDataToImport(newDefinition, record.fileName)

            if (newProcDefDto.format == CmmnFormat.ECOS_CMMN.code) {
                validateEcosCmmnFormat(newDefinition)
            }

            record.ecosType = newProcDefDto.ecosTypeRef
            record.name = newProcDefDto.name
            record.processDefId = newProcDefDto.id
        }

        if (record.processDefId.isBlank()) {
            error("processDefId is missing")
        }

        val newRef = ProcDefRef.create(PROC_TYPE, record.processDefId)

        val currentProc = procDefService.getProcessDefById(newRef)

        if (record.id.isNotBlank() && record.id != record.processDefId && currentProc != null) {
            error("Process definition with id " + newRef.id + " already exists")
        }

        if (currentProc == null) {

            if (newProcDefDto != null) {

                procDefService.uploadProcDef(newProcDefDto)
            } else {

                val newProcDef = NewProcessDefDto()

                newProcDef.id = record.processDefId

                val format = if (record.format.isNotBlank()) {
                    CmmnFormat.getByCode(record.format)
                } else {
                    CmmnFormat.ECOS_CMMN
                }

                val defData = when (format) {
                    CmmnFormat.ECOS_CMMN -> {
                        mapper.toBytes(
                            CmmnIO.generateDefaultDef(record.processDefId, record.name, record.ecosType)
                        )
                    }
                    CmmnFormat.LEGACY_CMMN -> {
                        val def = CmmnIO.generateLegacyDefaultTemplate(
                            record.processDefId,
                            record.name,
                            record.ecosType
                        )
                        CmmnXmlUtils.writeToBytes(def)
                    }
                }

                newProcDef.name = record.name
                newProcDef.data = defData
                newProcDef.ecosTypeRef = record.ecosType
                newProcDef.format = format.code
                newProcDef.procType = PROC_TYPE

                procDefService.uploadProcDef(newProcDef)
            }
        } else {

            if (newProcDefDto != null) {

                currentProc.data = newProcDefDto.data
                currentProc.ecosTypeRef = newProcDefDto.ecosTypeRef
                currentProc.name = newProcDefDto.name
            } else {

                currentProc.ecosTypeRef = record.ecosType
                currentProc.name = record.name
                currentProc.enabled = record.enabled

                if (currentProc.format == CmmnFormat.ECOS_CMMN.code) {

                    var procDef = mapper.read(currentProc.data, CmmnProcessDef::class.java)
                        ?: error("Process parsing error: ${currentProc.id}")

                    procDef = procDef.copy {
                        withEcosType(record.ecosType)
                        withName(record.name)
                    }
                    currentProc.data = mapper.toBytes(procDef) ?: error("Process conversion error: ${currentProc.id}")
                } else {

                    val definition = CmmnXmlUtils.readFromBytes(currentProc.data)
                    definition.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE] = record.ecosType.toString()
                    definition.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = mapper.toString(record.name)
                    currentProc.data = CmmnXmlUtils.writeToBytes(definition)
                }
            }

            procDefService.uploadNewRev(currentProc)
        }

        return record.processDefId
    }

    private fun validateEcosCmmnFormat(newDefinition: String) {
        val cmmnProcDef = CmmnIO.importEcosCmmn(newDefinition)
        CmmnIO.exportEcosCmmn(cmmnProcDef)
    }

    override fun delete(recordId: String): DelStatus {
        procDefService.delete(ProcDefRef.create(PROC_TYPE, recordId))
        return DelStatus.OK
    }

    override fun getId() = SOURCE_ID

    inner class CmmnProcDefRecord(
        private val procDef: ProcDefDto
    ) {

        fun getEcosType(): RecordRef {
            return procDef.ecosTypeRef
        }

        fun getId(): String {
            return procDef.id
        }

        fun getName(): MLText {
            return procDef.name ?: MLText()
        }

        fun getFormat(): String {
            return procDef.format
        }

        fun getJournalFormat(): CmmnFormatType? {
            return try {
                when (CmmnFormat.getByCode(procDef.format)) {
                    CmmnFormat.LEGACY_CMMN ->
                        CmmnFormatType("old", "Таблица")
                    CmmnFormat.ECOS_CMMN ->
                        CmmnFormatType("new", "Схема")
                }
            } catch (e: Exception) {
                null
            }
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
            if (rev.format == CmmnFormat.ECOS_CMMN.code) {
                return mapper.read(rev.data, CmmnProcessDef::class.java)?.let {
                    try {
                        CmmnIO.exportEcosCmmnToString(it)
                    } catch (e: Exception) {
                        log.error("Definition reading failed: ${rev.procDefId} ${rev.id} ${e.message}", e)

                        null
                    }
                }
            }
            return String(rev.data, StandardCharsets.UTF_8)
        }

        @AttName("?json")
        fun getJson(): CmmnProcessDef? {
            val rev = procDefService.getProcessDefRev(PROC_TYPE, procDef.revisionId) ?: return null
            if (rev.format != CmmnFormat.ECOS_CMMN.code) {
                error("Json representation allowed only for ECOS_CMMN processes")
            }
            return mapper.read(rev.data, CmmnProcessDef::class.java)
        }

        @AttName("_type")
        fun getType(): RecordRef {
            return RecordRef.create("emodel", "type", "cmmn-process-def")
        }
    }

    data class CmmnFormatType(
        val value: String,
        val displayName: String
    )

    class CmmnMutateRecord(
        var id: String,
        var processDefId: String,
        var name: MLText,
        var ecosType: RecordRef,
        var definition: String? = null,
        var enabled: Boolean,
        var fileName: String = "",
        var format: String = ""
    ) {

        @JsonProperty("_content")
        fun setContent(contentList: List<ObjectData>) {

            fileName = contentList[0].get("originalName").asText()

            val base64Content = contentList[0].get("url").asText()
            val contentRegex = "^data:(.+?);base64,(.+)$".toRegex()
            val dataMatch = contentRegex.matchEntire(base64Content) ?: error("Incorrect content: $base64Content")

            val format = dataMatch.groupValues[1]
            val contentText = String(Base64.getDecoder().decode(dataMatch.groupValues[2]))

            definition = when (format) {
                "text/xml" -> {
                    contentText
                }
                "application/json" -> {
                    val def = mapper.read(contentText, CmmnProcessDef::class.java)
                        ?: error("Incorrect content: $base64Content")
                    CmmnIO.exportEcosCmmnToString(def)
                }
                else -> {
                    error("Unknown format: $format")
                }
            }
        }
    }
}
