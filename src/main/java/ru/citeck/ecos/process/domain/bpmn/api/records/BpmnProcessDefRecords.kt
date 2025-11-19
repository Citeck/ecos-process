package ru.citeck.ecos.process.domain.bpmn.api.records

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.camunda.bpm.engine.RepositoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.commons.utils.DataUriUtil
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.dto.PermissionType
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.process.common.section.perms.RootSectionPermsComponent
import ru.citeck.ecos.process.common.section.records.SectionsProxyDao
import ru.citeck.ecos.process.domain.bpmn.BPMN_FORMAT
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.BPMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_BPMN_SECTION
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.BpmnEventSubscriptionService
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.process.domain.bpmnreport.model.ReportElement
import ru.citeck.ecos.process.domain.bpmnreport.service.BpmnProcessReportService
import ru.citeck.ecos.process.domain.bpmnsection.BpmnSectionPermissionsProvider
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEvent
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEventEmitter
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
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
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.model.perms.ModelRecordPermsComponent
import ru.citeck.ecos.webapp.lib.perms.EcosPermissionsService
import ru.citeck.ecos.webapp.lib.perms.RecordPerms
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsComponent
import ru.citeck.ecos.webapp.lib.spring.context.content.EcosContentService
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

private const val PERMS_READ = "read"
private const val PERMS_WRITE = "write"
private const val ATT_SECTION_REF_ID = "sectionRef?id"

@Component
class BpmnProcessDefRecords(
    private val bpmnMutateDataProcessor: BpmnMutateDataProcessor,
    private val procDefService: ProcDefService,
    private val camundaRepoService: RepositoryService,
    private val remoteWebAppsApi: EcosRemoteWebAppsApi,
    private val procDefEventEmitter: ProcDefEventEmitter,
    private val bpmnEventSubscriptionService: BpmnEventSubscriptionService,
    private val bpmnProcessReportService: BpmnProcessReportService,
    private val ecosContentService: EcosContentService,
    private val bpmnSectionPermissionsProvider: BpmnSectionPermissionsProvider,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val workspaceService: WorkspaceService,
    private val bpmnIO: BpmnIO,
    private val procUtils: ProcUtils,
    ecosPermissionsService: EcosPermissionsService,
    customRecordPermsComponent: CustomRecordPermsComponent,
    modelRecordPermsComponent: ModelRecordPermsComponent,
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordAttsDao,
    RecordDeleteDao,
    RecordMutateDtoDao<BpmnProcessDefRecords.BpmnMutateRecord> {

    companion object {
        const val ID = "bpmn-def"

        private const val APP_NAME = AppName.EPROC
        private const val QUERY_BATCH_SIZE = 100
        private const val LIMIT_REQUESTS_COUNT = 100000
        private const val DEFAULT_MAX_ITEMS = 10000000

        val DEFAULT_SECTION_REF = SectionType.BPMN.getRef("DEFAULT")

        private val log = KotlinLogging.logger {}
    }

    @Volatile
    private var allProcDefsFromAlfresco: List<AlfProcDefRecord> = emptyList()
    @Volatile
    private var allProcDefsFromAlfrescoNextUpdateTime = Instant.EPOCH

    private val permsCalculator = ecosPermissionsService.createCalculator()
        .withoutDefaultComponents()
        .addComponent(RootSectionPermsComponent())
        .addComponent(modelRecordPermsComponent)
        .addComponent(customRecordPermsComponent)
        .allowAllForAdmins()
        .build()

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        if (recsQuery.language == "predicate-with-data") {
            return loadDefinitionsByPredicateWithDataFromAlfresco(recsQuery)
        }

        if (recsQuery.language != PredicateService.LANGUAGE_PREDICATE) {
            return null
        }
        val queryPredicate = recsQuery.getQuery(Predicate::class.java)

        val predicate = Predicates.and(
            queryPredicate,
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
                recsQuery.workspaces,
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
            log.warn { "Request count limit reached! Request: $recsQuery" }
        }

        if (skip <= result.size) {
            result.subList(0, skip).clear()
        } else {
            result.clear()
        }

        var totalCount = procDefService.getCount(recsQuery.workspaces, predicate)

        val isMoreRecordsRequired = recsQuery.page.maxItems == -1 ||
            result.size < (recsQuery.page.maxItems + recsQuery.page.skipCount)

        val isSearchInGlobalWs = recsQuery.workspaces.isEmpty() ||
            recsQuery.workspaces.any { workspaceService.isWorkspaceWithGlobalEntities(it) }

        if (isMoreRecordsRequired && remoteWebAppsApi.isAppAvailable(AppName.ALFRESCO) && isSearchInGlobalWs) {

            // maybe should be changed to checking predicate directly without DTO
            val queryDto = PredicateUtils.convertToDto(
                queryPredicate,
                ProcDefQueryPredicateDto::class.java,
                true
            )
            var alfDefinitions = loadAllDefinitionsFromAlfresco()
            if (queryDto.sectionRef.isNotEmpty()) {
                alfDefinitions = alfDefinitions.filter { it.getSectionRef() == queryDto.sectionRef }
            }
            totalCount += alfDefinitions.size
            result.addAll(alfDefinitions)
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
        return EntityRef.create(APP_NAME, ID, this)
    }

    private fun EntityRef.getPerms(): ProcDefPermsValue {
        return ProcDefPermsValue(this, SectionType.BPMN)
    }

    private fun loadAllDefinitionsFromAlfresco(): List<AlfProcDefRecord> {

        if (allProcDefsFromAlfrescoNextUpdateTime.isAfter(Instant.now())) {
            return allProcDefsFromAlfresco
        }

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
        allProcDefsFromAlfresco = processes.getRecords().map {
            if (StringUtils.isBlank(it.sectionRef) ||
                it.sectionRef == "workspace://SpacesStore/cat-doc-kind-ecos-bpm-default"
            ) {

                it.sectionRef = DEFAULT_BPMN_SECTION
            } else if (it.sectionRef?.startsWith("workspace://SpacesStore/") == true) {

                it.sectionRef = "${EprocApp.NAME}/bpmn-section@" + it.sectionRef?.substringAfterLast('/')
            }
            AlfProcDefRecord(it)
        }
        allProcDefsFromAlfrescoNextUpdateTime = Instant.now().plusSeconds(60)
        return allProcDefsFromAlfresco
    }

    override fun getRecordAtts(recordId: String): Any? {

        if (recordId.startsWith("flowable$") || recordId.startsWith("activiti$")) {
            return EntityRef.create("alfresco", "workflow", "def_$recordId")
        }

        // TODO: remove atts hack after fix in perms request (should run as sys)
        val atts = AttContext.getInnerAttsMap().values
        val allowRead = if (AuthContext.isRunAsSystem()) {
            true
        } else if (atts.size == 1 && StringUtils.startsWithAny(
                atts.first(),
                "?id",
                "permissions",
                "_permissions",
                "_type",
                "_status",
                "sectionRef"
            )
        ) {
            true
        } else {
            recordId.toProcDefRef().getPerms().hasReadPerms()
        }

        if (!allowRead) {
            return null
        }

        val ref = ProcDefRef.create(BPMN_PROC_TYPE, workspaceService.convertToIdInWs(recordId))
        val currentProc = procDefService.getProcessDefById(ref)

        return currentProc?.let {
            BpmnProcessDefRecord(
                ProcDefDto(
                    it.id,
                    it.name,
                    it.procType,
                    it.workspace,
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

    override fun getRecToMutate(recordId: String): BpmnMutateRecord {
        return if (recordId.isBlank()) {
            BpmnMutateRecord(
                id = "",
                processDefId = "",
                name = MLText(),
                ecosType = EntityRef.EMPTY,
                formRef = EntityRef.EMPTY,
                workingCopySourceRef = EntityRef.EMPTY,
                definition = null,
                enabled = false,
                action = "",
                autoStartEnabled = false,
                autoDeleteEnabled = true,
                sectionRef = EntityRef.EMPTY,
                imageBytes = null
            )
        } else {
            val procDef = procDefService.getProcessDefById(
                ProcDefRef.create(BPMN_PROC_TYPE, workspaceService.convertToIdInWs(recordId))
            ) ?: error("Process definition is not found: $recordId")
            BpmnMutateRecord(
                id = recordId,
                processDefId = procDef.id,
                workspace = procDef.workspace,
                name = procDef.name ?: MLText(),
                ecosType = procDef.ecosTypeRef,
                formRef = procDef.formRef,
                workingCopySourceRef = procDef.workingCopySourceRef,
                definition = null,
                enabled = procDef.enabled,
                action = "",
                autoStartEnabled = procDef.autoStartEnabled,
                autoDeleteEnabled = procDef.autoDeleteEnabled,
                sectionRef = procDef.sectionRef,
                imageBytes = procDef.image,
                moduleId = recordId
            )
        }
    }

    override fun saveMutatedRec(record: BpmnMutateRecord): String {

        if (record.action == BpmnProcessDefActions.VALIDATE_GENERAL_BPMN.name) {
            val definition = record.definition ?: ""
            require(definition.isNotBlank()) {
                "BPMN definition cannot be empty for validation"
            }

            bpmnIO.validateEcosBpmnFormat(definition)

            return "ok"
        }

        val procDefRef = record.id.toProcDefRef()
        val perms = if (record.isNewRecord) {
            ProcDefPermsValue(record, SectionType.BPMN)
        } else {
            procDefRef.getPerms()
        }

        val sectionRef = record.sectionRef.ifEmpty { DEFAULT_SECTION_REF }
        record.sectionRef = sectionRef

        if (sectionRef.getLocalId() == "ROOT") {
            error("You can't create processes in ROOT category")
        }

        if (AuthContext.isNotRunAsSystemOrAdmin()) {

            if (record.isNewRecord || record.sectionRef != record.sectionRefBefore) {

                val isNonGlobalWs = !workspaceService.isWorkspaceWithGlobalEntities(record.workspace)
                val hasPermissionToCreateDefinitionsInSection = if (isNonGlobalWs) {
                    sectionRef.getLocalId() == SectionsProxyDao.SECTION_DEFAULT
                } else {
                    bpmnSectionPermissionsProvider.hasPermissions(
                        sectionRef,
                        BpmnPermission.SECTION_CREATE_PROC_DEF
                    )
                }

                if (!hasPermissionToCreateDefinitionsInSection) {
                    error("Permission denied. You can't create process instances in section $sectionRef")
                }

                if (isNonGlobalWs &&
                    !workspaceService.isUserManagerOf(AuthContext.getCurrentUser(), record.workspace)
                ) {
                    error("Permission denied. You can't create process instances in workspace '${record.workspace}'")
                }
            }
            if (!record.isNewRecord && !perms.hasWritePerms()) {
                error("Permissions denied. You can't edit process definition: $procDefRef")
            }
        }

        val mutData = bpmnMutateDataProcessor.getCompletedMutateData(record)

        val newRef = ProcDefRef.create(BPMN_PROC_TYPE, IdInWs.create(mutData.workspace, mutData.processDefId))
        val currentProc = procDefService.getProcessDefById(newRef)
        val procDefResult: ProcDefDto

        if (record.processDefId.isBlank()) {
            record.processDefId = mutData.recordId
        }

        val recordIdInWs = workspaceService.convertToIdInWs(mutData.recordId)
        if ((recordIdInWs.id != mutData.processDefId) && currentProc != null) {
            error("Process definition with id " + newRef.idInWs + " already exists")
        }

        // create new process definition
        if (currentProc == null) {

            val newProcDef = NewProcessDefDto(
                id = mutData.processDefId,
                workspace = mutData.workspace,
                enabled = mutData.enabled,
                autoStartEnabled = mutData.autoStartEnabled,
                autoDeleteEnabled = mutData.autoDeleteEnabled,
                name = mutData.name,
                data = mutData.newEcosDefinition
                    ?: BpmnXmlUtils.writeToString(bpmnIO.generateDefaultDef(mutData)).toByteArray(),
                ecosTypeRef = mutData.ecosType,
                formRef = mutData.formRef,
                workingCopySourceRef = mutData.workingCopySourceRef,
                format = BPMN_FORMAT,
                procType = BPMN_PROC_TYPE,
                sectionRef = mutData.sectionRef,
                image = mutData.image
            )

            procDefResult = if (mutData.saveAsDraft) {
                procDefService.uploadProcDefDraft(newProcDef)
            } else {
                procDefService.uploadProcDef(newProcDef)
            }
        } else {
            // update existing process definition
            currentProc.ecosTypeRef = mutData.ecosType
            currentProc.formRef = mutData.formRef
            currentProc.workingCopySourceRef = mutData.workingCopySourceRef
            currentProc.name = mutData.name
            currentProc.enabled = mutData.enabled
            currentProc.autoStartEnabled = mutData.autoStartEnabled
            currentProc.autoDeleteEnabled = mutData.autoDeleteEnabled
            currentProc.sectionRef = mutData.sectionRef
            currentProc.createdFromVersion = mutData.createdFromVersion
            currentProc.image = mutData.image

            if (mutData.newEcosDefinition != null) {
                // update process definition from bpmn editor
                currentProc.data = mutData.newEcosDefinition
            } else {
                // update process definition from form
                if (currentProc.format == BPMN_FORMAT) {

                    val procDef = BpmnXmlUtils.readFromString(String(currentProc.data))
                    procDef.otherAttributes[BPMN_PROP_NAME_ML] = mapper.toString(mutData.name)
                    procDef.otherAttributes[BPMN_PROP_ECOS_TYPE] = mutData.ecosType.toString()
                    procDef.otherAttributes[BPMN_PROP_FORM_REF] = mutData.formRef.toString()
                    procDef.otherAttributes[BPMN_PROP_WORKING_COPY_SOURCE_REF] =
                        mutData.workingCopySourceRef.toString()
                    procDef.otherAttributes[BPMN_PROP_ENABLED] = mutData.enabled.toString()
                    procDef.otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = mutData.autoStartEnabled.toString()
                    procDef.otherAttributes[BPMN_PROP_AUTO_DELETE_ENABLED] = mutData.autoDeleteEnabled.toString()
                    procDef.otherAttributes[BPMN_PROP_SECTION_REF] = mutData.sectionRef.toString()

                    currentProc.data = BpmnXmlUtils.writeToString(procDef).toByteArray()
                }
            }

            procDefResult = if (mutData.saveAsDraft) {
                procDefService.uploadNewDraftRev(currentProc, record.comment, record.forceMajorVersion)
            } else {
                procDefService.uploadNewRev(currentProc, record.comment)
            }
        }
        if (record.action == BpmnProcessDefActions.DEPLOY.toString()) {
            check(perms.hasDeployPerms()) {
                "Permissions denied for deploy: $procDefRef"
            }
            check(record.processDefId.isNotBlank()) {
                "Process definition id cannot be blank for $procDefRef"
            }

            log.debug { "Deploy to camunda:\n ${mutData.newCamundaDefinitionStr}" }

            var resName = record.processDefId
            var eventWorkspace = ""
            if (!workspaceService.isWorkspaceWithGlobalEntities(record.workspace)) {
                resName = workspaceService.getWorkspaceSystemId(record.workspace) +
                    ProcUtils.PROC_KEY_WS_DELIM + resName
                eventWorkspace = record.workspace
            }
            resName += BPMN_RESOURCE_NAME_POSTFIX

            val deployResult = ProcUtils.doWithWorkspaceContext(eventWorkspace) {
                camundaRepoService.createDeployment()
                    .addInputStream(resName, mutData.newCamundaDefinitionStr.byteInputStream())
                    .name(record.name.getClosest())
                    .source("Ecos BPMN Modeler")
                    .deployWithResult()
            }

            procDefService.saveProcessDefRevDeploymentId(procDefResult.revisionId, deployResult.id)
            bpmnEventSubscriptionService.addSubscriptionsForDefRev(procDefResult.revisionId)

            procDefEventEmitter.emitProcDefDeployed(
                ProcDefEvent(
                    procDefRef = procDefRef,
                    version = procDefResult.version.toDouble().inc(),
                    dataState = procDefResult.dataState.name,
                    workspace = eventWorkspace
                )
            )

            log.debug { "Camunda deploy result: $deployResult" }
        }

        return workspaceService.addWsPrefixToId(record.processDefId, record.workspace)
    }

    override fun delete(recordId: String): DelStatus {
        return if (recordId.toProcDefRef().getPerms().hasWritePerms()) {
            procDefService.delete(ProcDefRef.create(BPMN_PROC_TYPE, workspaceService.convertToIdInWs(recordId)))
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

        private val permsValue by lazy { ProcDefPermsValue(getRef(), SectionType.BPMN) }
        private val revision: ProcDefRevDto? by lazy {
            procDefService.getProcessDefRev(BPMN_PROC_TYPE, procDef.revisionId)
        }

        @AttName(ScalarType.ID_SCHEMA)
        fun getRef(): EntityRef {
            val localId = workspaceService.addWsPrefixToId(procDef.id, procDef.workspace)
            return EntityRef.create(AppName.EPROC, ID, localId)
        }

        @AttName("_permissions")
        fun getPermissionsSysAtt(): ProcDefPermsValue {
            return permsValue
        }

        fun getPermissions(): ProcDefPermsValue {
            return getPermissionsSysAtt()
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
        fun getDisplayName(): MLText {
            return getName()
        }

        fun getEnabled(): Boolean {
            return procDef.enabled
        }

        fun getAutoStartEnabled(): Boolean {
            return procDef.autoStartEnabled
        }

        fun getAutoDeleteEnabled(): Boolean {
            return procDef.autoDeleteEnabled
        }

        fun getData(): ByteArray? {
            return getDefinition()?.toByteArray(StandardCharsets.UTF_8)
        }

        fun getArtifactId() = procDef.id

        fun getModuleId() = procDef.id

        fun getProcessDefId() = procDef.id

        fun getDefinition(): String? {
            return revision?.let {
                String(it.loadData(procDefRevDataProvider), StandardCharsets.UTF_8)
            }
        }

        @AttName("revisionId")
        fun revisionId(): String {
            return procDef.revisionId.toString()
        }

        @AttName("deploymentId")
        fun getDeploymentId(): String {
            return revision?.deploymentId ?: ""
        }

        @AttName("?json")
        fun getJson(): BpmnDefinitionDef? {
            error("Json representation is not supported")
        }

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "bpmn-process-def")
        }

        @AttName("startFormRef")
        fun getStartFormRef(): EntityRef {
            return procDef.formRef
        }

        @AttName("formRef")
        fun getFormRef(): EntityRef {
            return procDef.formRef
        }

        @AttName("workingCopySourceRef")
        fun getWorkingCopySourceRef(): EntityRef {
            return procDef.workingCopySourceRef
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

        fun getWorkspace(): String {
            return procDef.workspace
        }

        fun getPreview(): EprocBpmnPreviewValue {
            return EprocBpmnPreviewValue(procDef.id, procDef.modified.toEpochMilli())
        }

        fun getBpmnReport(): List<ReportElement>? {
            val def = getDefinition()?.let { bpmnIO.importEcosBpmn(it, validate = false) }
            return def?.let { bpmnProcessReportService.generateReportElementListForBpmnDefinition(it) }
        }

        fun getSectionPath(): List<SectionPathPart> {
            val getAtts: (EntityRef) -> SectionDto = {
                recordsService.getAtts(it, SectionDto::class.java)
            }

            val getInfoByDto: (SectionDto) -> SectionPathPart = { section ->
                SectionPathPart(
                    code = section.sectionCode?.takeIf { it.isNotBlank() },
                    name = section.name
                )
            }

            val result = ArrayList<SectionPathPart>()
            var sectionDto = getAtts(procDef.sectionRef)
            result.add(getInfoByDto(sectionDto))

            var currentRef = sectionDto.parent

            while (currentRef != null) {
                sectionDto = getAtts(currentRef)
                result.add(getInfoByDto(sectionDto))
                currentRef = sectionDto.parent
            }

            return result.reversed()
        }
    }

    inner class ProcDefPermsValue(
        private val record: Any,
        private val sectionType: SectionType
    ) : AttValue {

        private val currentUser = AuthContext.getCurrentUser()
        private val recordPerms: RecordPerms by lazy {
            permsCalculator.getPermissions(record)
        }
        private val workspace: String by lazy {
            AuthContext.runAsSystem {
                recordsService.getAtt(record, "workspace").asText()
            }
        }
        private val isCurrentUserManagerOfWs: Boolean by lazy {
            workspace.isNotBlank() && AuthContext.runAsSystem {
                workspaceService.isUserManagerOf(currentUser, workspace)
            }
        }

        private val sectionPerms: RecordPerms by lazy {
            var sectionRef = recordsService.getAtt(record, ATT_SECTION_REF_ID).asText().toEntityRef()
            if (EntityRef.isEmpty(sectionRef)) {
                sectionRef = EntityRef.create(AppName.EPROC, sectionType.sourceId, "DEFAULT")
            }
            permsCalculator.getPermissions(sectionRef)
        }

        override fun has(name: String): Boolean {
            if (AuthContext.isRunAsSystem()) {
                return true
            }
            if (!workspaceService.isWorkspaceWithGlobalEntities(workspace) && isCurrentUserManagerOfWs) {
                return true
            }
            var hasPermission = recordPerms.hasPermission(name)
            if (!hasPermission) {
                hasPermission = sectionPerms.hasPermission(name)
            }
            if (!hasPermission && name.equals(PermissionType.WRITE.name, true)) {
                hasPermission = sectionPerms.hasPermission(sectionType.editInSectionPermissionId)
            }
            return hasPermission
        }

        fun hasReadPerms(): Boolean {
            if (AuthContext.isRunAsSystem()) {
                return true
            }
            if (!workspaceService.isWorkspaceWithGlobalEntities(workspace)) {
                return AuthContext.runAsSystem {
                    workspaceService.isUserMemberOf(currentUser, workspace)
                }
            }
            return has(PERMS_READ)
        }

        fun hasWritePerms(): Boolean {
            if (AuthContext.isRunAsSystem()) {
                return true
            }
            if (!workspaceService.isWorkspaceWithGlobalEntities(workspace)) {
                return isCurrentUserManagerOfWs
            }
            return has(PERMS_WRITE)
        }

        fun hasDeployPerms(): Boolean {
            return has(sectionType.deployPermissionId)
        }
    }

    class EprocBpmnPreviewValue(val id: String?, private val cacheBust: Any?) {

        fun getUrl(): String {
            val ref = EntityRef.create(EprocApp.NAME, ID, id).toString()
            return "/gateway/eproc/api/procdef/preview?ref=$ref&cb=$cacheBust"
        }
    }

    inner class BpmnMutateRecord(
        var id: String,
        var processDefId: String,
        var name: MLText,
        var ecosType: EntityRef,
        var formRef: EntityRef,
        var workingCopySourceRef: EntityRef?,
        var definition: String? = null,
        var enabled: Boolean,
        var action: String = "",
        var autoStartEnabled: Boolean,
        var autoDeleteEnabled: Boolean,
        var sectionRef: EntityRef?,
        var imageBytes: ByteArray?,
        var createdFromVersion: EntityRef = EntityRef.EMPTY,
        var moduleId: String? = null,
        var forceMajorVersion: Boolean = false,
        var comment: String = "",
        var isUploadNewVersion: Boolean = false,
        var workspace: String = ""
    ) {
        val sectionRefBefore = sectionRef.ifEmpty { DEFAULT_SECTION_REF }
        val isNewRecord = id.isBlank()

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "bpmn-process-def")
        }

        @JsonProperty("version:version")
        fun applyUploadNewVersionState(version: String) {
            if (version.isNotBlank()) {
                forceMajorVersion = true

                isUploadNewVersion = id.isNotBlank()
            }
        }

        @JsonProperty("version:comment")
        fun setVersionComment(comment: String) {
            this.comment = comment
        }

        @JsonProperty(RecordConstants.ATT_WORKSPACE)
        fun setCtxWorkspace(workspace: String?) {
            this.workspace = procUtils.getUpdatedWsInMutation(this.workspace, workspace)
        }

        fun setImage(imageUrl: String) {
            imageBytes = DataUriUtil.parseData(imageUrl).data
        }

        @JsonProperty("_content")
        fun setContent(content: Any) {
            definition = when (content) {
                is String -> {
                    if (content.trim().startsWith("<?xml")) {
                        content
                    } else {
                        ecosContentService.getContent(EntityRef.valueOf(content))?.readContentAsText()
                            ?: error("Content is not found: $content")
                    }
                }

                is List<*> -> {
                    val firsElement: Any? = content[0]

                    val contentData = ObjectData.create(firsElement)
                    val base64Content = contentData.get("url", "")
                    val contentRegex = "^data:(.+?);base64,(.+)$".toRegex()
                    val dataMatch =
                        contentRegex.matchEntire(base64Content) ?: error("Incorrect content: $base64Content")

                    val format = dataMatch.groupValues[1]
                    when (format) {
                        "text/xml" -> {
                            String(Base64.getDecoder().decode(dataMatch.groupValues[2]))
                        }

                        "application/json" -> {
                            error("Json is not supported")
                        }

                        else -> {
                            error("Unknown format: $format")
                        }
                    }
                }

                else -> {
                    error("Unknown content type: ${content::class.java}")
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
        val startFormRef: EntityRef?,
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

    data class SectionDto(
        val sectionCode: String?,
        @AttName("name?json")
        val name: MLText?,
        @AttName("parentRef?id")
        val parent: EntityRef?
    )

    inner class SectionPathPart(
        val code: String?,
        var name: MLText?
    )

    class ProcDefQueryPredicateDto(
        val sectionRef: EntityRef = EntityRef.EMPTY
    )
}
