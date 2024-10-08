package ru.citeck.ecos.process.domain.cmmn.api.records

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.cmmn.io.CmmnFormat
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.cmmn.io.CmmnProcDefImporter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
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
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CmmnProcDefRecords(
    private val procDefService: ProcDefService,
    private val cmmnProcDefImporter: CmmnProcDefImporter,
    private val procDefRevDataProvider: ProcDefRevDataProvider
) : RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<CmmnProcDefRecords.CmmnMutateRecord> {

    companion object {
        const val SOURCE_ID = "cmmn-def"
        const val CMMN_PROC_TYPE = "cmmn"

        private val log = KotlinLogging.logger {}
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = Predicates.and(
            recsQuery.getQuery(Predicate::class.java),
            Predicates.eq("procType", CMMN_PROC_TYPE)
        )

        val result = procDefService.findAll(
            predicate,
            recsQuery.page.maxItems,
            recsQuery.page.skipCount
        ).map { CmmnProcDefRecord(it) }

        val res = RecsQueryRes(result)
        res.setTotalCount(procDefService.getCount(predicate))
        res.setHasMore(res.getTotalCount() > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return res
    }

    override fun getRecordAtts(recordId: String): Any? {

        val ref = ProcDefRef.create(CMMN_PROC_TYPE, recordId)
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let {
            CmmnProcDefRecord(
                ProcDefDto(
                    it.id,
                    it.name,
                    it.procType,
                    it.format,
                    it.revisionId,
                    it.version,
                    it.ecosTypeRef,
                    it.alfType,
                    it.formRef,
                    it.workingCopySourceRef,
                    it.enabled,
                    it.autoStartEnabled,
                    it.autoDeleteEnabled,
                    it.sectionRef,
                    it.created,
                    it.modified
                )
            )
        } ?: EmptyAttValue.INSTANCE
    }

    override fun getRecToMutate(recordId: String): CmmnMutateRecord {
        return if (recordId.isBlank()) {
            CmmnMutateRecord("", "", MLText(), EntityRef.EMPTY, null, true)
        } else {
            val procDef = procDefService.getProcessDefById(ProcDefRef.create(CMMN_PROC_TYPE, recordId))
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

        val newRef = ProcDefRef.create(CMMN_PROC_TYPE, record.processDefId)

        val currentProc = procDefService.getProcessDefById(newRef)

        if (record.id.isNotBlank() && record.id != record.processDefId && currentProc != null) {
            error("Process definition with id " + newRef.id + " already exists")
        }

        if (currentProc == null) {

            if (newProcDefDto != null) {

                procDefService.uploadProcDef(newProcDefDto)
            } else {

                val format = if (record.format.isNotBlank()) {
                    CmmnFormat.getByCode(record.format)
                } else {
                    CmmnFormat.ECOS_CMMN
                }

                val defData = when (format) {
                    CmmnFormat.ECOS_CMMN -> {
                        mapper.toBytes(
                            CmmnIO.generateDefaultDef(record.processDefId, record.name, record.ecosType)
                        ) ?: error("Incorrect format. Record: $record")
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

                val newProcDef = NewProcessDefDto(
                    id = record.processDefId,
                    name = record.name,
                    data = defData,
                    ecosTypeRef = record.ecosType,
                    format = format.code,
                    procType = CMMN_PROC_TYPE,
                    image = null,
                    enabled = record.enabled
                )

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
        procDefService.delete(ProcDefRef.create(CMMN_PROC_TYPE, recordId))
        return DelStatus.OK
    }

    override fun getId() = SOURCE_ID

    inner class CmmnProcDefRecord(
        private val procDef: ProcDefDto
    ) {

        fun getEcosType(): EntityRef {
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
            return MLText.getClosestValue(getName(), I18nContext.getLocale())
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
            val rev = procDefService.getProcessDefRev(CMMN_PROC_TYPE, procDef.revisionId) ?: return null
            if (rev.format == CmmnFormat.ECOS_CMMN.code) {
                return mapper.read(rev.loadData(procDefRevDataProvider), CmmnProcessDef::class.java)?.let {
                    try {
                        CmmnIO.exportEcosCmmnToString(it)
                    } catch (e: Exception) {
                        log.error("Definition reading failed: ${rev.procDefId} ${rev.id} ${e.message}", e)

                        null
                    }
                }
            }
            return String(rev.loadData(procDefRevDataProvider), StandardCharsets.UTF_8)
        }

        @AttName("?json")
        fun getJson(): CmmnProcessDef? {
            val rev = procDefService.getProcessDefRev(CMMN_PROC_TYPE, procDef.revisionId) ?: return null
            if (rev.format != CmmnFormat.ECOS_CMMN.code) {
                error("Json representation allowed only for ECOS_CMMN processes")
            }
            return mapper.read(rev.loadData(procDefRevDataProvider), CmmnProcessDef::class.java)
        }

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "cmmn-process-def")
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
        var ecosType: EntityRef,
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
