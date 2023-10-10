package ru.citeck.ecos.process.domain.bpmn.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonProperty
import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.utils.DataUriUtil
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.BPMN_FORMAT
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.BPMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_BPMN_SECTION
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.BpmnEventSubscriptionService
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEvent
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEventEmitter
import ru.citeck.ecos.process.domain.procdef.perms.ProcDefPermsValue
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
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
import ru.citeck.ecos.webapp.api.apps.EcosRemoteWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

const val BPMN_PROCESS_DEF_RECORDS_SOURCE_ID = "bpmn-def"

@Component
class BpmnProcessDefRecords(
    private val bpmnMutateDataProcessor: BpmnMutateDataProcessor,
    private val procDefService: ProcDefService,
    private val camundaRepoService: RepositoryService,
    private val remoteWebAppsApi: EcosRemoteWebAppsApi,
    private val procDefEventEmitter: ProcDefEventEmitter,
    private val bpmnEventSubscriptionService: BpmnEventSubscriptionService
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<BpmnProcessDefRecords.BpmnMutateRecord> {

    companion object {

        private const val APP_NAME = AppName.EPROC
        private const val QUERY_BATCH_SIZE = 100
        private const val LIMIT_REQUESTS_COUNT = 100000
        private const val DEFAULT_MAX_ITEMS = 10000000

        private val log = KotlinLogging.logger {}
    }

    override fun getId() = BPMN_PROCESS_DEF_RECORDS_SOURCE_ID

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language == "predicate-with-data") {
            return loadDefinitionsByPredicateWithDataFromAlfresco(recsQuery)
        }

        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }

        val predicate = Predicates.and(
            recsQuery.getQuery(Predicate::class.java),
            Predicates.eq("procType", BPMN_PROC_TYPE)
        )

        var numberOfExecutedRequests = 0
        val maxItems = if (recsQuery.page.maxItems > 0) {
            recsQuery.page.maxItems
        } else {
            DEFAULT_MAX_ITEMS
        }
        val skip = recsQuery.page.skipCount
        var requiredAmount = maxItems + skip
        val result = mutableListOf<Any>()
        var numberOfPermissionsCheck = 0
        do {
            val unfilteredBatch = procDefService.findAll(
                predicate,
                QUERY_BATCH_SIZE,
                QUERY_BATCH_SIZE * numberOfExecutedRequests++
            )

            val checkedRecords: List<BpmnProcessDefRecord>
            if (AuthContext.isRunAsSystem()) {
                checkedRecords = unfilteredBatch.map { BpmnProcessDefRecord(it) }
                    .take(requiredAmount)
                numberOfPermissionsCheck = checkedRecords.size
            } else {
                checkedRecords = unfilteredBatch.asSequence()
                    .map { BpmnProcessDefRecord(it) }
                    .filter {
                        numberOfPermissionsCheck++
                        it.getPermissions().hasReadPerms()
                    }
                    .take(requiredAmount)
                    .toList()
            }

            result.addAll(checkedRecords)
            requiredAmount -= checkedRecords.size
            val hasMore = unfilteredBatch.size == QUERY_BATCH_SIZE
        } while (hasMore &&
            requiredAmount != 0 &&
            numberOfExecutedRequests < LIMIT_REQUESTS_COUNT
        )

        if (numberOfExecutedRequests == LIMIT_REQUESTS_COUNT) {
            log.warn("Request count limit reached! Request: $recsQuery")
        }

        if (skip <= result.size) {
            result.subList(0, skip).clear()
        } else {
            result.clear()
        }

        var totalCount = procDefService.getCount(predicate)

        val isMoreRecordsRequired = recsQuery.page.maxItems == -1 ||
            result.size < (recsQuery.page.maxItems + recsQuery.page.skipCount)

        if (isMoreRecordsRequired && remoteWebAppsApi.isAppAvailable(AppName.ALFRESCO)) {

            val alfDefinitions = loadAllDefinitionsFromAlfresco()
            result.addAll(alfDefinitions)
            totalCount += alfDefinitions.size
        }

        val res = RecsQueryRes(result)
        res.setTotalCount(totalCount)
        res.setHasMore(res.getTotalCount() > recsQuery.page.maxItems + recsQuery.page.skipCount)

        log.trace { "Perms check count of bpmn proc def: $numberOfPermissionsCheck" }

        return res
    }

    private fun loadDefinitionsByPredicateWithDataFromAlfresco(query: RecordsQuery): Any {

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

    private fun String.toProcDefRef(): EntityRef {
        return EntityRef.create(APP_NAME, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, this)
    }

    private fun EntityRef.getPerms(): ProcDefPermsValue {
        return ProcDefPermsValue(this)
    }

    private fun loadAllDefinitionsFromAlfresco(): List<AlfProcDefRecord> {
        val predicate = Predicates.and(
            Predicates.eq("type", "ecosbpm:processModel"),
        )
        val processes = recordsService.query(
            RecordsQuery.create {
                withSourceId("alfresco/")
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withMaxItems(300)
            },
            ProcDefAlfAtts::class.java
        )
        return processes.getRecords().map {
            if (StringUtils.isBlank(it.sectionRef) ||
                it.sectionRef == "workspace://SpacesStore/cat-doc-kind-ecos-bpm-default"
            ) {

                it.sectionRef = DEFAULT_BPMN_SECTION
            } else if (it.sectionRef?.startsWith("workspace://SpacesStore/") == true) {

                it.sectionRef = "${EprocApp.NAME}/bpmn-section@" + it.sectionRef?.substringAfterLast('/')
            }
            AlfProcDefRecord(it)
        }
    }

    override fun getRecordAtts(recordId: String): Any? {

        if (recordId.startsWith("flowable$") || recordId.startsWith("activiti$")) {
            return RecordRef.create("alfresco", "workflow", "def_$recordId")
        }

        val ref = ProcDefRef.create(BPMN_PROC_TYPE, recordId)
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let {
            BpmnProcessDefRecord(
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

    override fun getRecToMutate(recordId: String): BpmnMutateRecord {
        return if (recordId.isBlank()) {
            BpmnMutateRecord(
                "",
                "",
                MLText(),
                RecordRef.EMPTY,
                RecordRef.EMPTY,
                null,
                false,
                "",
                false,
                EntityRef.EMPTY,
                null
            )
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
                procDef.autoStartEnabled,
                procDef.sectionRef,
                procDef.image
            )
        }
    }

    override fun saveMutatedRec(record: BpmnMutateRecord): String {
        val procDefRef = record.id.toProcDefRef()
        val perms = procDefRef.getPerms()

        if (!perms.hasWritePerms()) {
            error("Permissions denied for mutate: $procDefRef")
        }

        val mutateData = bpmnMutateDataProcessor.getCompletedMutateData(record)

        with(mutateData) {
            val newRef = ProcDefRef.create(BPMN_PROC_TYPE, processDefId)
            val currentProc = procDefService.getProcessDefById(newRef)
            val procDefResult: ProcDefDto

            if ((recordId != processDefId) && currentProc != null) {
                error("Process definition with id " + newRef.id + " already exists")
            }

            // create new process definition
            if (currentProc == null) {

                val newProcDef = NewProcessDefDto(
                    id = processDefId,
                    enabled = enabled,
                    autoStartEnabled = autoStartEnabled,
                    name = name,
                    data = newEcosDefinition ?: BpmnXmlUtils.writeToString(
                        BpmnIO.generateDefaultDef(mutateData)
                    ).toByteArray(),
                    ecosTypeRef = ecosType,
                    formRef = formRef,
                    format = BPMN_FORMAT,
                    procType = BPMN_PROC_TYPE,
                    sectionRef = sectionRef,
                    image = image
                )

                procDefResult = if (saveAsDraft) {
                    procDefService.uploadProcDefDraft(newProcDef)
                } else {
                    procDefService.uploadProcDef(newProcDef)
                }
            } else {
                // update existing process definition
                currentProc.ecosTypeRef = ecosType
                currentProc.formRef = formRef
                currentProc.name = name
                currentProc.enabled = enabled
                currentProc.autoStartEnabled = autoStartEnabled
                currentProc.sectionRef = sectionRef
                currentProc.createdFromVersion = createdFromVersion
                currentProc.image = image

                if (newEcosDefinition != null) {
                    // update process definition from bpmn editor
                    currentProc.data = newEcosDefinition
                } else {
                    // update process definition from form
                    if (currentProc.format == BPMN_FORMAT) {

                        val procDef = BpmnXmlUtils.readFromString(String(currentProc.data))
                        procDef.otherAttributes[BPMN_PROP_NAME_ML] = mapper.toString(name)
                        procDef.otherAttributes[BPMN_PROP_ECOS_TYPE] = ecosType.toString()
                        procDef.otherAttributes[BPMN_PROP_FORM_REF] = formRef.toString()
                        procDef.otherAttributes[BPMN_PROP_ENABLED] = enabled.toString()
                        procDef.otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = autoStartEnabled.toString()
                        procDef.otherAttributes[BPMN_PROP_SECTION_REF] = sectionRef.toString()

                        currentProc.data = BpmnXmlUtils.writeToString(procDef).toByteArray()
                    }
                }

                procDefResult = if (saveAsDraft) {
                    procDefService.uploadNewDraftRev(currentProc)
                } else {
                    procDefService.uploadNewRev(currentProc)
                }
            }

            if (record.action == BpmnProcessDefActions.DEPLOY.toString()) {
                if (!perms.hasDeployPerms()) {
                    error("Permissions denied for deploy: $procDefRef")
                }

                log.debug { "Deploy to camunda:\n $newCamundaDefinitionStr" }

                val deployResult = camundaRepoService.createDeployment()
                    .addInputStream(
                        record.processDefId + BPMN_RESOURCE_NAME_POSTFIX,
                        newCamundaDefinitionStr.byteInputStream()
                    )
                    .name(record.name.getClosest())
                    .source("Ecos BPMN Modeler")
                    .deployWithResult()

                procDefService.saveProcessDefRevDeploymentId(procDefResult.revisionId, deployResult.id)
                bpmnEventSubscriptionService.addSubscriptionsForDefRev(procDefResult.revisionId)

                procDefEventEmitter.emitProcDefDeployed(
                    ProcDefEvent(
                        procDefRef = procDefRef,
                        version = procDefResult.version.toDouble().inc(),
                        dataState = procDefResult.dataState.name
                    )
                )

                log.debug { "Camunda deploy result: $deployResult" }
            }

            return record.processDefId
        }
    }

    override fun delete(recordId: String): DelStatus {
        return if (recordId.toProcDefRef().getPerms().hasWritePerms()) {
            procDefService.delete(ProcDefRef.create(BPMN_PROC_TYPE, recordId))
            DelStatus.OK
        } else {
            DelStatus.PROTECTED
        }
    }

    inner class AlfProcDefRecord(
        private val alfAtts: ProcDefAlfAtts
    ) {

        fun getDisplayName(): MLText? {
            return alfAtts.getDisplayName()
        }

        fun getEcosType(): EntityRef {
            return EntityRef.EMPTY
        }

        fun getId(): EntityRef {
            return EntityRef.create(AppName.ALFRESCO, "", alfAtts.nodeRef)
        }

        fun getPreview(): Any {
            if (!alfAtts.hasThumbnail) {
                return EprocBpmnPreviewValue(null, "")
            }
            return AlfPreviewValue(alfAtts)
        }

        fun getSectionRef(): EntityRef {
            return alfAtts.sectionRef.toEntityRef()
        }

        @AttName(RecordConstants.ATT_MODIFIED)
        fun getModified(): Instant {
            return alfAtts.modified
        }

        @AttName(RecordConstants.ATT_CREATED)
        fun getCreated(): Instant {
            return alfAtts.created
        }
    }

    private class AlfPreviewValue(
        val alfAtts: ProcDefAlfAtts
    ) {
        fun getUrl(): String {
            return "/gateway/alfresco/alfresco/s/citeck/ecos/image/thumbnail" +
                "?nodeRef=${alfAtts.nodeRef}" +
                "&property=ecosbpm:thumbnail" +
                "&cached=true" +
                "&modified=${alfAtts.modified.toEpochMilli()}"
        }
    }

    inner class BpmnProcessDefRecord(
        private val procDef: ProcDefDto
    ) {

        private val permsValue by lazy { ProcDefPermsValue(getId().toProcDefRef()) }

        fun getPermissions(): ProcDefPermsValue {
            return permsValue
        }

        fun getEcosType(): EntityRef {
            return procDef.ecosTypeRef
        }

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
        fun getType(): EntityRef {
            return EntityRef.create("emodel", "type", "bpmn-process-def")
        }

        @AttName("startFormRef")
        fun getStartFormRef(): EntityRef {
            return procDef.formRef
        }

        @AttName("formRef")
        fun getFormRef(): EntityRef {
            return procDef.formRef
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

        fun getPreview(): EprocBpmnPreviewValue {
            return EprocBpmnPreviewValue(procDef.id, procDef.modified.toEpochMilli())
        }
    }

    class EprocBpmnPreviewValue(val id: String?, private val cacheBust: Any?) {

        fun getUrl(): String {
            val ref = EntityRef.create(EprocApp.NAME, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, id).toString()
            return "/gateway/eproc/api/procdef/preview?ref=$ref&cb=$cacheBust"
        }
    }

    class BpmnMutateRecord(
        var id: String,
        var processDefId: String,
        var name: MLText,
        var ecosType: EntityRef,
        var formRef: EntityRef,
        var definition: String? = null,
        var enabled: Boolean,
        var action: String = "",
        var autoStartEnabled: Boolean,
        var sectionRef: EntityRef,
        var imageBytes: ByteArray?,
        var createdFromVersion: EntityRef = EntityRef.EMPTY
    ) {

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

        override fun toString(): String {
            return "BpmnMutateRecord(id='$id', processDefId='$processDefId', name=$name)"
        }
    }

    class ProcDefAlfQuery(
        val data: ObjectData,
        val predicate: Predicate
    )

    class ProcDefAlfAtts(
        @AttName("ecosbpm:sectionRef?id!ecosbpm:category?localId")
        var sectionRef: String?,
        @AttName("ecosbpm:processId")
        val processId: String?,
        @AttName("ecosbpm:engine")
        val engine: String?,
        @AttName("cm:title")
        val title: MLText?,
        val startFormRef: RecordRef?,
        @AttName("_has.ecosbpm:thumbnail?bool!false")
        val hasThumbnail: Boolean,
        @AttName(RecordConstants.ATT_MODIFIED)
        val modified: Instant,
        @AttName(RecordConstants.ATT_CREATED)
        val created: Instant,
        @AttName("?localId")
        val nodeRef: String
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
