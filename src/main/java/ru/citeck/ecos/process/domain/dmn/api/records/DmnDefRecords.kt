package ru.citeck.ecos.process.domain.dmn.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.DataUriUtil
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.permissions.service.RecordPermsService
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.dmn.DMN_FORMAT
import ru.citeck.ecos.process.domain.dmn.DMN_PROC_TYPE
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_SECTION_REF
import ru.citeck.ecos.process.domain.dmn.io.DmnIO
import ru.citeck.ecos.process.domain.dmn.io.xml.DmnXmlUtils
import ru.citeck.ecos.process.domain.dmn.model.ecos.DmnDefinitionDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEvent
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEventEmitter
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.impl.EmptyAttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
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
import java.time.Instant
import java.util.*

const val DMN_DEF_SOURCE_ID = "dmn-def"

@Component
class DmnDefRecords(
    private val procDefService: ProcDefService,
    private val recordPermsService: RecordPermsService,
    private val roleService: RoleService,
    private val procDefEventEmitter: ProcDefEventEmitter,
    private val camundaRepoService: RepositoryService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<DmnDefRecords.DmnMutateRecord> {

    companion object {
        private const val QUERY_BATCH_SIZE = 100
        private const val LIMIT_REQUESTS_COUNT = 100000
        private const val DEFAULT_MAX_ITEMS = 10000000

        private val DMN_DEF_TYPE = TypeUtils.getTypeRef("dmn-def")

        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return DMN_DEF_SOURCE_ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = Predicates.and(
            recsQuery.getQuery(Predicate::class.java),
            Predicates.eq("procType", DMN_PROC_TYPE)
        )

        var numberOfExecutedRequests = 0
        val maxItems = if (recsQuery.page.maxItems > 0) {
            recsQuery.page.maxItems
        } else {
            DEFAULT_MAX_ITEMS
        }
        var requiredAmount = maxItems
        val result = mutableListOf<Any>()
        var numberOfPermissionsCheck = 0

        do {
            val unfilteredBatch = procDefService.findAll(
                predicate,
                QUERY_BATCH_SIZE,
                recsQuery.page.skipCount + QUERY_BATCH_SIZE * numberOfExecutedRequests++
            )

            val checkedRecords: List<DmnDefRecord>
            if (AuthContext.isRunAsSystem()) {
                checkedRecords = unfilteredBatch.map { it.toDmnRecord() }
                    .take(requiredAmount)
                numberOfPermissionsCheck = checkedRecords.size
            } else {
                checkedRecords = unfilteredBatch.asSequence()
                    .map { it.toDmnRecord() }
                    .filter {
                        numberOfPermissionsCheck++
                        it.hasReadPerms()
                    }
                    .take(requiredAmount)
                    .toList()
            }

            result.addAll(checkedRecords)
            requiredAmount = maxItems - result.size
            val hasMore = unfilteredBatch.size == QUERY_BATCH_SIZE
        } while (hasMore && requiredAmount != 0 && numberOfExecutedRequests < LIMIT_REQUESTS_COUNT)

        if (numberOfExecutedRequests == LIMIT_REQUESTS_COUNT) {
            log.warn("Request count limit reached! Request: $recsQuery")
        }

        var totalCount = procDefService.getCount(predicate)

        val res = RecsQueryRes(result)
        res.setTotalCount(totalCount)
        res.setHasMore(res.getTotalCount() > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return res
    }

    override fun getRecToMutate(recordId: String): DmnMutateRecord {
        return if (recordId.isBlank()) {
            DmnMutateRecord.EMPTY
        } else {
            val procDef = procDefService.getProcessDefById(ProcDefRef.create(DMN_PROC_TYPE, recordId))
                ?: error("Process definition is not found: $recordId")
            DmnMutateRecord(
                recordId,
                recordId,
                procDef.name ?: MLText(),
                null,
                "",
                procDef.sectionRef,
                procDef.image
            )
        }
    }

    override fun saveMutatedRec(record: DmnMutateRecord): String {
        val dmnRef = record.id.toDmnRef()
        if (!dmnRef.hasWritePerms()) {
            throw RuntimeException("Permissions denied. RecordRef: $dmnRef")
        }

        val newDefinition = record.definition ?: ""
        var newDefData: ByteArray? = null
        var newEcosDmnDef: DmnDefinitionDef? = null

        if (newDefinition.isNotBlank()) {

            validateEcosDmnFormat(newDefinition)

            newEcosDmnDef = DmnIO.importEcosDmn(newDefinition)

            if (log.isDebugEnabled) {
                val ecosDmnStr = DmnIO.exportEcosDmnToString(newEcosDmnDef)
                log.debug { "exportEcosDmnToString:\n$ecosDmnStr" }

                val camundaStr = DmnIO.exportCamundaDmnToString(newEcosDmnDef)
                log.debug { "exportCamundaDmnToString:\n$camundaStr" }
            }

            newDefData = DmnIO.exportEcosDmnToString(newEcosDmnDef).toByteArray()

            if (record.id.isBlank()) {
                record.id = record.defId
            }
        }

        if (record.defId.isBlank()) {
            error("defId is missing")
        }

        val newRef = ProcDefRef.create(DMN_PROC_TYPE, record.defId)

        val currentProc = procDefService.getProcessDefById(newRef)
        val procDefResult: ProcDefDto

        if ((record.id != record.defId) && currentProc != null) {
            error("Process definition with id " + newRef.id + " already exists")
        }

        if (currentProc == null) {

            val newProcDef = NewProcessDefDto(
                id = record.defId,
                name = record.name,
                data = newDefData ?: DmnXmlUtils.writeToString(
                    DmnIO.generateDefaultDef(record.defId, record.name)
                ).toByteArray(),
                format = DMN_FORMAT,
                procType = DMN_PROC_TYPE,
                sectionRef = record.sectionRef,
                image = record.imageBytes
            )

            procDefResult = procDefService.uploadProcDef(newProcDef)
        } else {

            if (newDefData != null) {

                currentProc.data = newDefData
                currentProc.name = record.name
                currentProc.sectionRef = record.sectionRef
                currentProc.createdFromVersion = record.createdFromVersion
                currentProc.image = record.imageBytes
            } else {

                currentProc.name = record.name
                currentProc.sectionRef = record.sectionRef
                currentProc.createdFromVersion = record.createdFromVersion
                currentProc.image = record.imageBytes

                if (currentProc.format == DMN_FORMAT) {

                    val procDef = DmnXmlUtils.readFromString(String(currentProc.data))
                    procDef.otherAttributes[DMN_PROP_NAME_ML] = Json.mapper.toString(record.name)
                    procDef.otherAttributes[DMN_PROP_SECTION_REF] = record.sectionRef.toString()

                    currentProc.data = DmnXmlUtils.writeToString(procDef).toByteArray()
                }
            }

            procDefResult = procDefService.uploadNewRev(currentProc)
        }

        if (record.action == DmnDefActions.DEPLOY.toString()) {
            // TODO: move to separate service

            val camundaFormat = DmnIO.exportCamundaDmnToString(newEcosDmnDef!!)
            log.debug { "Deploy to camunda:\n $camundaFormat" }

            val deployResult = camundaRepoService.createDeployment()
                .addInputStream(record.defId + ".dmn", camundaFormat.byteInputStream())
                .name(record.name.getClosest())
                .source("Ecos DMN Modeler")
                .deployWithResult()

            procDefService.saveProcessDefRevDeploymentId(procDefResult.revisionId, deployResult.id)

            procDefEventEmitter.emitProcDefDeployed(
                ProcDefEvent(
                    procDefRef = dmnRef,
                    version = procDefResult.version.toDouble().inc()
                )
            )

            log.debug { "Camunda deploy result: $deployResult" }
        }

        return record.defId
    }

    private fun validateEcosDmnFormat(newDefinition: String) {
        val ecosDmnDef = DmnIO.importEcosDmn(newDefinition)
        DmnIO.exportEcosDmn(ecosDmnDef)
        DmnIO.exportCamundaDmn(ecosDmnDef)
    }

    private fun ProcDefDto.toDmnRecord(): DmnDefRecord {
        return DmnDefRecord(this)
    }

    private fun DmnDefRecord.hasReadPerms(): Boolean {
        val dmnDefRef = this.getId().toDmnRef()
        val recordPerms = recordPermsService.getRecordPerms(dmnDefRef)
        return recordPerms?.isReadAllowed(dmnDefRef.getRoles()) ?: false
    }

    private fun EntityRef.hasWritePerms(): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }
        if (this.isNewRecord() && AuthContext.isRunAsAdmin()) {
            return true
        }
        val recordPerms = recordPermsService.getRecordPerms(this)
        return recordPerms?.isWriteAllowed(this.getRoles()) ?: false
    }

    private fun EntityRef.isNewRecord(): Boolean {
        return this.getLocalId().isEmpty()
    }

    private fun EntityRef.getRoles(): List<String> {
        return roleService.getRolesId(DMN_DEF_TYPE)
            .filter { roleService.isRoleMember(this, it) }
            .toList()
    }

    private fun String.toDmnRef(): EntityRef {
        return EntityRef.create(AppName.EPROC, DMN_DEF_SOURCE_ID, this)
    }

    override fun getRecordAtts(recordId: String): Any? {
        val ref = ProcDefRef.create(DMN_PROC_TYPE, recordId)
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let {
            DmnDefRecord(
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
                    it.enabled,
                    it.autoStartEnabled,
                    it.sectionRef,
                    it.created,
                    it.modified
                )
            )
        } ?: EmptyAttValue.INSTANCE
    }

    override fun delete(recordId: String): DelStatus {
       val ref = recordId.toDmnRef()
        return if (ref.hasWritePerms()) {
            procDefService.delete(ProcDefRef.create(DMN_PROC_TYPE, ref.getLocalId()))
            DelStatus.OK
        } else {
            DelStatus.PROTECTED
        }
    }

    inner class DmnDefRecord(
        private val procDef: ProcDefDto
    ) {

        fun getId(): String {
            return procDef.id
        }

        fun getName(): MLText {
            val name = procDef.name
            if (MLText.isEmpty(name)) {
                return MLText(procDef.id)
            }
            return name
        }

        fun getFormat(): String {
            return procDef.format
        }

        @AttName("?disp")
        fun getDisplayName(): String {
            return MLText.getClosestValue(getName(), I18nContext.getLocale())
        }

        fun getData(): ByteArray? {
            return getDefinition()?.toByteArray(StandardCharsets.UTF_8)
        }

        fun getArtifactId() = procDef.id

        fun getModuleId() = procDef.id

        fun getDefId() = procDef.id

        fun getDefinition(): String? {
            val rev = procDefService.getProcessDefRev(DMN_PROC_TYPE, procDef.revisionId) ?: return null
            return String(rev.data, StandardCharsets.UTF_8)
        }

        @AttName("?json")
        fun getJson(): DmnDefinitionDef? {
            error("Json representation is not supported")
        }

        @AttName("_type")
        fun getType(): EntityRef {
            return EntityRef.create("emodel", "type", "dmn-def")
        }

        @AttName(RecordConstants.ATT_CREATED)
        fun getCreated(): Instant {
            return procDef.created
        }

        @AttName(RecordConstants.ATT_MODIFIED)
        fun getModified(): Instant {
            return procDef.modified
        }

        fun getSectionRef(): EntityRef {
            return procDef.sectionRef
        }

        fun getPreview(): EprocDmnPreviewValue {
            return EprocDmnPreviewValue(procDef.id, procDef.modified.toEpochMilli())
        }
    }

    class EprocDmnPreviewValue(val id: String?, private val cacheBust: Any?) {

        fun getUrl(): String {
            val ref = EntityRef.create(EprocApp.NAME, DMN_DEF_SOURCE_ID, id).toString()
            return "/gateway/eproc/api/procdef/preview?ref=$ref&cb=$cacheBust"
        }
    }


    class DmnMutateRecord(
        var id: String,
        var defId: String,
        var name: MLText,
        var definition: String? = null,
        var action: String = "",
        var sectionRef: EntityRef,
        var imageBytes: ByteArray?,
        var createdFromVersion: EntityRef = EntityRef.EMPTY
    ) {

        companion object {
            val EMPTY = DmnMutateRecord("", "", MLText(), "", "", EntityRef.EMPTY, null)
        }

        fun setImage(imageUrl: String) {
            imageBytes = DataUriUtil.parseData(imageUrl).data
        }

        @JsonProperty("_content")
        fun setContent(contentList: List<ObjectData>) {

            val base64Content = contentList[0].get("url", "")
            val contentRegex = "^data:(.+?);base64,(.+)$".toRegex()
            val dataMatch = contentRegex.matchEntire(base64Content) ?: error("Incorrect content: $base64Content")

            val format = dataMatch.groupValues[1]
            val contentText = String(Base64.getDecoder().decode(dataMatch.groupValues[2]))

            definition = when (format) {
                "text/xml" -> {
                    contentText
                }

                "application/json" -> {
                    error("Json is not supported")
                }

                else -> {
                    error("Unknown format: $format")
                }
            }
        }
    }
}
