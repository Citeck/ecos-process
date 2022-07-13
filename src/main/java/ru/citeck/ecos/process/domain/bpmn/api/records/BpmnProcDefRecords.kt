package ru.citeck.ecos.process.domain.bpmn.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.model.lib.permissions.service.RecordPermsService
import ru.citeck.ecos.model.lib.role.constants.RoleConstants
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.bpmn.BPMN_FORMAT
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
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
import ru.citeck.ecos.webapp.api.apps.EcosWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class BpmnProcDefRecords(
    val procDefService: ProcDefService,
    val repositoryService: RepositoryService,
    val webAppsApi: EcosWebAppsApi,
    val recordPermsService: RecordPermsService,
    val roleService: RoleService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<BpmnProcDefRecords.BpmnMutateRecord> {

    companion object {
        const val SOURCE_ID = "bpmn-def"
        private const val APP_NAME = AppName.EPROC
        private const val QUERY_BATCH_SIZE = 100
        private const val LIMIT_REQUESTS_COUNT = 100000
        private const val DEFAULT_MAX_ITEMS = 10000000

        private val log = KotlinLogging.logger {}
        private val BPMN_PROC_DEF_TYPE = TypeUtils.getTypeRef("bpmn-process-def")
    }

    override fun queryRecords(query: RecordsQuery): Any? {

        if (query.language == "predicate-with-data") {
            return loadDefinitionsFromAlfresco(query)
        }

        if (query.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = Predicates.and(
            query.getQuery(Predicate::class.java),
            Predicates.eq("procType", BPMN_PROC_TYPE)
        )

        var numberOfExecutedRequests = 0
        val maxItems = if (query.page.maxItems > 0) query.page.maxItems else DEFAULT_MAX_ITEMS
        var requiredAmount = maxItems
        var result = emptyArray<BpmnProcDefRecord>()
        var numberOfPermissionsCheck = 0
        do {
            val unfilteredBatch = procDefService.findAll(
                predicate,
                QUERY_BATCH_SIZE,
                query.page.skipCount + QUERY_BATCH_SIZE * numberOfExecutedRequests++
            )

            val checkedRecords: List<BpmnProcDefRecord>
            if (AuthContext.isRunAsSystem()) {
                checkedRecords = unfilteredBatch.map(::toBpmnProcDefRecord)
                    .take(requiredAmount)
                numberOfPermissionsCheck = checkedRecords.size
            } else {
                checkedRecords = unfilteredBatch.asSequence()
                    .map(::toBpmnProcDefRecord)
                    .filter {
                        numberOfPermissionsCheck++
                        hasReadPerms(it)
                    }
                    .take(requiredAmount)
                    .toList()
            }

            result = result.plus(checkedRecords)
            requiredAmount = maxItems - result.size
            val hasMore = unfilteredBatch.size == QUERY_BATCH_SIZE
        } while (hasMore &&
            requiredAmount != 0 &&
            numberOfExecutedRequests < LIMIT_REQUESTS_COUNT
        )

        if (numberOfExecutedRequests == LIMIT_REQUESTS_COUNT) {
            log.warn("Request count limit reached! Request: $query")
        }

        if (result.size < (query.page.maxItems + query.page.skipCount) &&
            webAppsApi.isAppAvailable(AppName.ALFRESCO)
        ) {

            // todo: query
        }

        val res = RecsQueryRes(result.toList())
        res.setTotalCount(procDefService.getCount(predicate))
        res.setHasMore(res.getTotalCount() > numberOfPermissionsCheck + query.page.skipCount)

        return res
    }

    private fun toBpmnProcDefRecord(procDefDto: ProcDefDto): BpmnProcDefRecord {
        return BpmnProcDefRecord(procDefDto)
    }

    private fun toRecordRef(id: String): RecordRef {
        return RecordRef.create(APP_NAME, SOURCE_ID, id)
    }

    private fun hasReadPerms(bpmnProcDefRecord: BpmnProcDefRecord): Boolean {
        val procDefRecordRef = toRecordRef(bpmnProcDefRecord.getId())
        val recordPerms = recordPermsService.getRecordPerms(procDefRecordRef)
        val roles = getRoles(procDefRecordRef)
        return recordPerms?.isReadAllowed(roles) ?: false
    }

    private fun hasWritePerms(recordRef: RecordRef): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }
        val recordPerms = recordPermsService.getRecordPerms(recordRef)
        val roles = getRoles(recordRef)
        return recordPerms?.isWriteAllowed(roles) ?: false
    }

    private fun getRoles(recordRef: RecordRef): List<String> {
        return roleService.getRolesId(BPMN_PROC_DEF_TYPE)
            .filter { roleService.isRoleMember(recordRef, it) }
            .toList()
            .plus(RoleConstants.ROLE_EVERYONE)
    }

    private fun loadDefinitionsFromAlfresco(query: RecordsQuery): Any {

        val procDefQuery = query.getQuery(ProcDefAlfQuery::class.java)

        val predicateForAlfresco = Predicates.and(
            Predicates.eq("type", "ecosbpm:processModel"),
            PredicateUtils.mapValuePredicates(procDefQuery.predicate) { pred ->
                when (pred.getAttribute()) {
                    "processDefId" -> ValuePredicate("ecosbpm:processId", pred.getType(), pred.getValue())
                    "name" -> ValuePredicate("cm:title", pred.getType(), pred.getValue())
                    else -> null
                }
            },
            Predicates.notEmpty("ecosbpm:startFormRef")
        )

        return recordsService.query(
            query.copy {
                withSourceId("alfresco/")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicateForAlfresco)
            },
            ProcDefAlfAtts::class.java
        )
    }

    override fun getRecordAtts(recordId: String): Any? {

        if (recordId.startsWith("flowable$") || recordId.startsWith("activiti$")) {
            return RecordRef.create("alfresco", "workflow", "def_$recordId")
        }

        val ref = ProcDefRef.create(BPMN_PROC_TYPE, recordId)
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let {
            BpmnProcDefRecord(
                ProcDefDto(
                    it.id,
                    it.name,
                    it.procType,
                    it.format,
                    it.revisionId,
                    it.ecosTypeRef,
                    it.alfType,
                    it.formRef,
                    it.enabled,
                    it.autoStartEnabled
                )
            )
        } ?: EmptyAttValue.INSTANCE
    }

    override fun getRecToMutate(recordId: String): BpmnMutateRecord {
        return if (recordId.isBlank()) {
            BpmnMutateRecord("", "", MLText(), RecordRef.EMPTY, RecordRef.EMPTY, null, false, "", false)
        } else {
            val procDef = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, recordId))
                ?: error("Process definition is not found: $recordId")
            BpmnMutateRecord(
                recordId,
                recordId,
                procDef.name ?: MLText(),
                procDef.ecosTypeRef,
                procDef.formRef,
                null,
                procDef.enabled,
                "",
                procDef.autoStartEnabled
            )
        }
    }

    override fun saveMutatedRec(record: BpmnMutateRecord): String {

        val recordRef = toRecordRef(record.id)
        if (!hasWritePerms(recordRef)) {
            throw RuntimeException("Permissions denied. RecordRef: $recordRef")
        }

        val newDefinition = record.definition ?: ""
        var newDefData: ByteArray? = null
        var newEcosBpmnDef: BpmnDefinitionDef? = null

        if (newDefinition.isNotBlank()) {

            validateEcosBpmnFormat(newDefinition)

            newEcosBpmnDef = BpmnIO.importEcosBpmn(newDefinition)

            // TODO: remove logging
            val s2 = BpmnIO.exportEcosBpmnToString(newEcosBpmnDef)
            log.info { "exportEcosBpmnToString:\n$s2" }
            val camundaStr = BpmnIO.exportCamundaBpmnToString(newEcosBpmnDef)
            log.info { "\nexportCamundaBpmnToString$camundaStr" }

            newDefData = BpmnIO.exportEcosBpmnToString(newEcosBpmnDef).toByteArray()

            record.ecosType = newEcosBpmnDef.ecosType
            record.formRef = newEcosBpmnDef.formRef
            record.name = newEcosBpmnDef.name
            record.processDefId = newEcosBpmnDef.id
            record.enabled = newEcosBpmnDef.enabled
            record.autoStartEnabled = newEcosBpmnDef.autoStartEnabled

            if (record.id.isBlank()) {
                record.id = record.processDefId
            }
        }

        if (record.processDefId.isBlank()) {
            error("processDefId is missing")
        }

        val newRef = ProcDefRef.create(BPMN_PROC_TYPE, record.processDefId)

        val currentProc = procDefService.getProcessDefById(newRef)

        if ((record.id != record.processDefId) && currentProc != null) {
            error("Process definition with id " + newRef.id + " already exists")
        }

        if (currentProc == null) {

            val newProcDef = NewProcessDefDto(
                id = record.processDefId,
                enabled = record.enabled,
                autoStartEnabled = record.autoStartEnabled,
                name = record.name,
                data = newDefData ?: BpmnXmlUtils.writeToString(
                    BpmnIO.generateDefaultDef(record.processDefId, record.name, record.ecosType)
                ).toByteArray(),
                ecosTypeRef = record.ecosType,
                formRef = record.formRef,
                format = BPMN_FORMAT,
                procType = BPMN_PROC_TYPE
            )

            procDefService.uploadProcDef(newProcDef)
        } else {

            if (newDefData != null) {

                currentProc.data = newDefData
                currentProc.ecosTypeRef = record.ecosType
                currentProc.formRef = record.formRef
                currentProc.name = record.name
                currentProc.enabled = record.enabled
                currentProc.autoStartEnabled = record.autoStartEnabled
            } else {

                currentProc.ecosTypeRef = record.ecosType
                currentProc.formRef = record.formRef
                currentProc.name = record.name
                currentProc.enabled = record.enabled
                currentProc.autoStartEnabled = record.autoStartEnabled

                if (currentProc.format == BPMN_FORMAT) {

                    val procDef = BpmnXmlUtils.readFromString(String(currentProc.data))
                    procDef.otherAttributes[BPMN_PROP_NAME_ML] = mapper.toString(record.name)
                    procDef.otherAttributes[BPMN_PROP_ECOS_TYPE] = record.ecosType.toString()
                    procDef.otherAttributes[BPMN_PROP_FORM_REF] = record.formRef.toString()
                    procDef.otherAttributes[BPMN_PROP_ENABLED] = record.enabled.toString()
                    procDef.otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = record.autoStartEnabled.toString()

                    currentProc.data = BpmnXmlUtils.writeToString(procDef).toByteArray()
                }
            }

            procDefService.uploadNewRev(currentProc)
        }

        if (record.action == BpmnProcDefActions.DEPLOY.toString()) {
            val camundaFormat = BpmnIO.exportCamundaBpmnToString(newEcosBpmnDef!!)
            log.debug { "Deploy to camunda:\n $camundaFormat" }

            val result = repositoryService.createDeployment()
                .addInputStream(record.processDefId + ".bpmn", camundaFormat.byteInputStream())
                .name(record.name.getClosest())
                .source("Ecos Modeler")
                .deployWithResult()

            log.debug { "Camunda deploy result: $result" }
        }

        return record.processDefId
    }

    private fun validateEcosBpmnFormat(newDefinition: String) {
        val ecosBpmnDef = BpmnIO.importEcosBpmn(newDefinition)
        BpmnIO.exportEcosBpmn(ecosBpmnDef)
        BpmnIO.exportCamundaBpmn(ecosBpmnDef)
    }

    override fun delete(recordId: String): DelStatus {
        val recordRef = toRecordRef(recordId)
        return if (hasWritePerms(recordRef)) {
            procDefService.delete(ProcDefRef.create(BPMN_PROC_TYPE, recordId))
            DelStatus.OK
        } else {
            DelStatus.PROTECTED
        }
    }

    override fun getId() = SOURCE_ID

    inner class BpmnProcDefRecord(
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

        @AttName("?disp")
        fun getDisplayName(): String {
            return MLText.getClosestValue(getName(), I18nContext.getLocale())
        }

        fun getEnabled(): Boolean {
            return procDef.enabled
        }

        fun getAutoStartEnabled(): Boolean {
            return procDef.autoStartEnabled
        }

        fun getData(): ByteArray? {
            return getDefinition()?.toByteArray(StandardCharsets.UTF_8)
        }

        fun getArtifactId() = procDef.id

        fun getModuleId() = procDef.id

        fun getProcessDefId() = procDef.id

        fun getDefinition(): String? {
            val rev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, procDef.revisionId) ?: return null
            return String(rev.data, StandardCharsets.UTF_8)
        }

        @AttName("?json")
        fun getJson(): BpmnDefinitionDef? {
            error("Json representation is not supported")
        }

        @AttName("_type")
        fun getType(): RecordRef {
            return BPMN_PROC_DEF_TYPE
        }

        @AttName("startFormRef")
        fun getStartFormRef(): RecordRef {
            return procDef.formRef
        }

        @AttName("formRef")
        fun getFormRef(): RecordRef {
            return procDef.formRef
        }
    }

    class BpmnMutateRecord(
        var id: String,
        var processDefId: String,
        var name: MLText,
        var ecosType: RecordRef,
        var formRef: RecordRef,
        var definition: String? = null,
        var enabled: Boolean,
        var action: String = "",
        var autoStartEnabled: Boolean
    ) {

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

    class ProcDefAlfQuery(
        val data: ObjectData,
        val predicate: Predicate
    )

    class ProcDefAlfAtts(
        @AttName("ecosbpm:processId")
        val processId: String?,
        @AttName("ecosbpm:engine")
        val engine: String?,
        @AttName("cm:title")
        val title: MLText?,
        val startFormRef: RecordRef?
    ) {
        fun getId(): String {
            return "$engine$$processId"
        }

        fun getProcessDefId(): String? {
            return processId
        }

        fun getProcType(): String? {
            return engine
        }

        fun getName(): MLText? {
            return title
        }

        fun getDisplayName() = getName()
    }
}
