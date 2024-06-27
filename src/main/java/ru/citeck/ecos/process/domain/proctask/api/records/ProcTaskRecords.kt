package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.apache.commons.lang3.time.FastDateFormat
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_STATUS
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_PREFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords.Companion.ALF_TASK_PREFIX
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.*
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Instant
import kotlin.system.measureTimeMillis

const val CHANGE_OWNER_RECORD_ACTION_CLAIM = "claim"
const val CHANGE_OWNER_RECORD_ACTION_RELEASE = "release"

const val CHANGE_OWNER_ATT = "changeOwner"
const val CHANGE_OWNER_ACTION_ATT = "action"
const val CHANGE_OWNER_USER_ATT = "owner"

private const val TASK_PERMISSION_REASSIGN = "Reassign"
private const val ATT_PREVIEW_INFO_JSON = "previewInfo${ScalarType.JSON_SCHEMA}"

@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService,
    private val procTaskOwnership: ProcTaskOwnership,
    private val ecosAuthoritiesApi: EcosAuthoritiesApi,
    private val taskActorsUtils: TaskActorsUtils,
    private val ecosTypesRegistry: EcosTypesRegistry,
    private val procTaskAttsSyncService: ProcTaskAttsSyncService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val taskConverter: TaskConverter
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDao {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val FORM_INFO_ATT = "_formInfo"

        const val ID = "proc-task"
        const val ALF_TASK_PREFIX = "workspace"

        val ALF_TO_ERPOC_TASK_ATTS = mapOf(
            "wfm:document" to "documentRef",
            "bpm:dueDate" to "dueDate",
            "bpm:priority" to "priority",
            "bpm:startDate" to "created",
            "cm:title" to "disp",
            "cm:name" to "id"
        )

        val EPROC_TO_ALF_TASK_ATTS = ALF_TO_ERPOC_TASK_ATTS.entries.associateBy({ it.value }) { it.key }
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {

        val predicate = if (recsQuery.language == PredicateService.LANGUAGE_PREDICATE) {
            recsQuery.getQuery(Predicate::class.java)
        } else if (recsQuery.language.isEmpty()) {
            Predicates.alwaysTrue()
        } else {
            error("Unsupported language: ${recsQuery.language}")
        }

        val sortBy = recsQuery.sortBy.ifEmpty {
            listOf(SortBy(RecordConstants.ATT_CREATED, false))
        }

        val tasksResult = procTaskService.findTasks(
            predicate,
            sortBy,
            recsQuery.page
        )
        val tasksRefs = tasksResult.entities.map { RecordRef.create(AppName.EPROC, ID, it) }

        val result = RecsQueryRes<RecordRef>()

        result.setRecords(tasksRefs)
        result.setTotalCount(tasksResult.totalCount)

        return result
    }

    override fun getRecordsAtts(recordIds: List<String>): List<ProcTaskRecord?>? {
        if (recordIds.isEmpty()) {
            return emptyList()
        }

        val result: List<ProcTaskRecord?>
        val resultTime = measureTimeMillis {
            val records = mutableMapOf<String, ProcTaskRecord?>()

            val procRefs = mutableListOf<String>()

            recordIds.forEach {
                val ref = RecordRef.valueOf(it)
                if (ref.isAlfTaskRef()) {
                    records[it] = createTaskRecordFromAlf(ref)
                } else {
                    procRefs.add(it)
                }
            }

            procTaskService.getTasksByIds(procRefs).map {
                val record: ProcTaskRecord?
                val toRecordTime = measureTimeMillis {
                    record = it?.let { taskDto -> taskConverter.toRecord(taskDto) }
                }

                log.trace { "To record: $toRecordTime ms" }

                record?.let { rec ->
                    records[rec.id] = rec
                }
            }

            result = recordIds.map { records[it] }
        }

        log.debug { "Get Camunda Tasks records atts: $resultTime ms" }

        return result
    }

    override fun mutate(record: LocalRecordAtts): String {
        val task = procTaskService.getTaskById(record.id) ?: throw IllegalArgumentException(
            "Task with id " + "${record.id} not found"
        )

        if (record.isChangeOwnerAction()) {
            val ownershipChangeData = record.toTaskOwnershipChangeData(record.id)
            procTaskOwnership.performAction(ownershipChangeData)
            return record.id
        }

        val mutateInfo = TaskMutateVariables(task, record)
        val outcome = getTaskOutcome(task, record)

        mutateDocumentVariables(task, mutateInfo.documentAtts)

        log.debug { "Submit task ${record.id} form with outcome: $outcome and variables: ${mutateInfo.taskVariables}" }

        procTaskService.completeTask(
            CompleteTaskData(
                task,
                outcome,
                mutateInfo.taskVariables
            )
        )

        return record.id
    }

    private fun LocalRecordAtts.isChangeOwnerAction(): Boolean {
        return getAtt(CHANGE_OWNER_ATT).isNotEmpty()
    }

    private fun LocalRecordAtts.toTaskOwnershipChangeData(taskId: String): TaskOwnershipChangeData {
        val changeOwner = getAtt(CHANGE_OWNER_ATT)
        val actionData = changeOwner[CHANGE_OWNER_ACTION_ATT].asText()
        val userData = changeOwner[CHANGE_OWNER_USER_ATT].asText()

        if (actionData.isBlank()) {
            error("Action type is mandatory for change owner action")
        }

        val action = let {
            if (actionData == CHANGE_OWNER_RECORD_ACTION_CLAIM) {
                if (userData.isBlank()) {
                    error("User is mandatory for `claim` owner action")
                }

                if (userData == CURRENT_USER_FLAG) {
                    return@let TaskOwnershipAction.CLAIM
                } else {
                    return@let TaskOwnershipAction.CHANGE
                }
            }
            if (actionData == CHANGE_OWNER_RECORD_ACTION_RELEASE) {
                return@let TaskOwnershipAction.UNCLAIM
            }

            error("Unknown action type: $actionData")
        }
        val user = if (userData == CURRENT_USER_FLAG) {
            AuthContext.getCurrentUser()
        } else {
            ecosAuthoritiesApi.getAuthorityName(userData)
        }

        return TaskOwnershipChangeData(action, taskId, user)
    }

    private fun getTaskOutcome(task: ProcTaskDto, record: LocalRecordAtts): Outcome {
        if (task.definitionKey.isNullOrBlank()) {
            throw IllegalStateException("Task DefinitionKey is mandatory for task completion")
        }

        var outcome = ""
        var formInfo = FormInfo()

        record.forEach { k, v ->
            if (k.startsWith(OUTCOME_PREFIX) && v.asBoolean()) {
                outcome = k.substringAfter(OUTCOME_PREFIX)
            }

            if (k == FORM_INFO_ATT) {
                formInfo = v.getAs(FormInfo::class.java) ?: FormInfo()
            }
        }

        if (outcome.isBlank()) {
            throw IllegalStateException("Task outcome is mandatory for task completion")
        }

        return Outcome(task.definitionKey, outcome, formInfo.submitName)
    }

    private fun mutateDocumentVariables(task: ProcTaskDto, documentAtts: RecordAtts) {
        if (task.documentRef != RecordRef.EMPTY && documentAtts.getAttributes().isNotEmpty()) {
            log.debug { "Submit task ${task.id}, mutate document <${task.documentRef}> with variables: $documentAtts" }
            recordsService.mutate(documentAtts)
        }
    }

    private fun createTaskRecordFromAlf(ref: RecordRef): ProcTaskRecord {
        val mapping = EPROC_TO_ALF_TASK_ATTS
        val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss")

        val alfAtts = let {
            if (!ref.isAlfTaskRef()) {
                return@let RecordAtts()
            }

            val attsMap = AttContext.getInnerAttsMap()
                .map {
                    if (mapping.containsKey(it.key)) {
                        val fixedAtt = mapping[it.key]!!
                        return@map it.key.replaceFirst(it.key, fixedAtt) to it.value.replaceFirst(it.key, fixedAtt)
                    }
                    it.key to it.value
                }
                .toMap()

            val fullOriginalRef = let {
                if (ref.appName.isBlank()) {
                    ref.withAppName("alfresco")
                } else {
                    ref
                }
            }

            recordsService.getAtts(fullOriginalRef, attsMap)
        }

        val getDateFromAtts = fun(att: String): Instant? {
            val data = alfAtts.getAtt(mapping[att]).asText()
            if (data.isBlank()) {
                return null
            }
            return dateFormat.parse(data).toInstant()
        }

        return ProcTaskRecord(
            id = ref.id,
            documentRef = RecordRef.valueOf(alfAtts.getAtt(mapping["documentRef"]).asText()),
            dueDate = getDateFromAtts("dueDate"),
            priority = alfAtts.getAtt(mapping["priority"]).asInt(),
            created = getDateFromAtts("created"),
            alfTaskAtts = alfAtts,
            title = MLText(alfAtts.getAtt(mapping["disp"]).asText()),
        )
    }

    inner class TaskMutateVariables(
        private val task: ProcTaskDto,
        record: LocalRecordAtts
    ) {
        val taskVariables = mutableMapOf<String, Any?>()
        val documentAtts = RecordAtts()

        init {
            log.debug { "Init TaskMutateVariables. atts: $record" }

            checkPermissionToCompleteTask()

            documentAtts.setId(task.documentRef)

            record.forEach { k, v ->
                when {
                    k.startsWith(DOCUMENT_FIELD_PREFIX) -> {
                        val docAtt = processDocumentVariable(k, v)
                        documentAtts[docAtt.first] = docAtt.second
                    }

                    k.startsWith(SYS_VAR_PREFIX) -> {
                        // do nothing
                    }

                    k.startsWith(OUTCOME_PREFIX) -> {
                        // do nothing
                    }

                    else -> {
                        taskVariables[k] = v.asJavaObj()
                    }
                }
            }
        }

        private fun checkPermissionToCompleteTask() {
            if (!taskActorsUtils.isCurrentUserTaskActorOrDelegate(task)) {
                throw IllegalStateException("Task mutate denied. Current user is not a task actor")
            }
        }

        private fun processDocumentVariable(k: String, v: DataValue): Pair<String, Any?> {
            return Pair(getEcmFieldName(k), v.asJavaObj())
        }

        private fun getEcmFieldName(name: String): String {
            return name.substring(DOCUMENT_FIELD_PREFIX.length)
                .replace("_".toRegex(), ":")
        }
    }

    inner class ProcTaskRecord(
        val id: String,
        val priority: Int = 0,
        val formRef: RecordRef = RecordRef.EMPTY,
        val processInstanceRef: RecordRef? = null,
        val documentRef: RecordRef? = null,
        val documentType: String? = null,
        val documentTypeRef: RecordRef? = null,
        val title: MLText? = null,
        val created: Instant? = null,
        val ended: Instant? = null,
        val durationInMillis: Long? = null,
        val dueDate: Instant? = null,
        val followUpDate: Instant? = null,
        val definitionKey: String? = null,

        val owner: EntityRef = RecordRef.EMPTY,
        val assignee: EntityRef = RecordRef.EMPTY,

        val senderTemp: EntityRef = RecordRef.EMPTY,

        val candidateUsers: List<EntityRef> = emptyList(),
        val candidateGroups: List<EntityRef> = emptyList(),

        val possibleOutcomes: List<TaskOutcome> = emptyList(),

        val comment: String? = null,
        val lastComment: String? = null,

        val documentAtts: RecordAtts = RecordAtts(),

        val alfTaskAtts: RecordAtts = RecordAtts(),

        val engineAtts: List<String> = emptyList(),

        val historic: Boolean = false,

        val reassignable: Boolean = false,
        val claimable: Boolean = false,
        val unclaimable: Boolean = false,
        val assignable: Boolean = false,
    ) {

        val permissions = object : AttValue {

            override fun has(name: String): Boolean {
                if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
                    return true
                }

                if (taskActorsUtils.isCurrentUserTaskActorOrDelegate(this@ProcTaskRecord)) {
                    return true
                }

                if (TASK_PERMISSION_REASSIGN.equals(name, ignoreCase = true)) {
                    return AuthContext.getCurrentAuthorities().contains(TASK_OWNERSHIP_REASSIGN_ALLOWED_GROUP)
                }

                log.warn { "Request unknown permission <$name> for task ${this@ProcTaskRecord.id}" }
                return false
            }
        }

        @get:AttName("_formRef")
        val formKey: RecordRef
            get() = if (formRef.isNotEmpty()) {
                formRef
            } else {
                RecordRef.create(AppName.UISERV, "form", "proc_task_default_form")
            }

        @get:AttName(RecordConstants.ATT_CREATED)
        val recordCreated: Instant?
            get() = created

        @AttName("started")
        fun getStarted(): Instant? {
            return created
        }

        @AttName("releasable")
        fun getReleasable(): Boolean {
            return unclaimable
        }

        // TODO: refactor task widget to using new api (request sender entityRef). Remove with method below.
        // Rename senderTemp to sender
        @AttName("sender")
        fun getSender(): AuthorityDto {
            return recordsService.getAtts(senderTemp, AuthorityDto::class.java)
        }

        @AttName("actors")
        fun getActors(): List<AuthorityDto> {
            val candidates = candidateUsers + candidateGroups

            val actorsRefs = if (EntityRef.isNotEmpty(assignee)) {
                listOf(assignee)
            } else {
                candidates
            }
            return recordsService.getAtts(actorsRefs, AuthorityDto::class.java)
        }

        @AttName(".disp")
        fun getDisp(): MLText? {
            return title
        }

        @AttName("name")
        fun getName(): MLText? {
            return title
        }

        @AttName("workflow")
        fun getWorkflow(): RecordRef? {
            return processInstanceRef
        }

        fun getPreviewInfo(): DataValue {
            return recordsService.getAtt(documentRef, ATT_PREVIEW_INFO_JSON)
        }

        @AttName("lastcomment")
        fun getLastCommentLegacyAtt(): String? {
            return lastComment
        }

        fun getAtt(name: String): Any? {
            val mapping = if (isAlfTask(id)) {
                ProcTaskRecords.EPROC_TO_ALF_TASK_ATTS
            } else {
                ProcTaskRecords.ALF_TO_ERPOC_TASK_ATTS
            }

            if (isAlfTask(id)) {
                if (mapping.containsKey(name)) {
                    val fixedAttName = mapping[name]
                    return alfTaskAtts.getAtt(fixedAttName)
                }

                return alfTaskAtts.getAtt(name)
            }

            if (mapping.containsKey(name)) {
                val fixedAttName = mapping[name]
                val attValue = when (fixedAttName) {
                    "documentRef" -> documentRef
                    "dueDate" -> dueDate
                    "priority" -> priority
                    "created" -> created
                    "disp" -> title
                    "id" -> id
                    else -> null
                }

                if (attValue != null) {
                    return attValue
                }
            }

            if (name.startsWith(DOCUMENT_FIELD_PREFIX)) {
                return documentAtts.getAtt(name.removePrefix(DOCUMENT_FIELD_PREFIX))
            }

            if (name.removePrefix(TASK_DOCUMENT_ATT_PREFIX) == BPMN_DOCUMENT_STATUS) {
                val status = procTaskService.getVariable(id, name) as String?
                val documentType = procTaskService.getVariable(id, BPMN_DOCUMENT_TYPE) as String?
                if (status.isNullOrBlank() || documentType.isNullOrBlank()) {
                    return null
                }

                val typeInfo = ecosTypesRegistry.getTypeInfo(ModelUtils.getTypeRef(documentType))

                return typeInfo?.model?.statuses?.find { it.id == status }
            }

            if (!historic && engineAtts.contains(name)) {
                // TODO: optimize this
                log.debug { "procTaskRecord $id request to task engine variables $id = $name. Possible performance issues" }

                val value = procTaskService.getVariable(id, name)
                val attType = procTaskAttsSyncService.getTaskAttTypeOrTextDefault(name)

                if (value is String) {
                    return when (attType) {
                        AttributeType.ASSOC -> value.toEntityRef()
                        AttributeType.AUTHORITY,
                        AttributeType.PERSON,
                        AttributeType.AUTHORITY_GROUP -> authoritiesApi.getAuthorityRef(value)

                        else -> value
                    }
                }

                return value
            }

            return null
        }
    }
}

fun RecordsQuery.getAttCreatedValuePredicate(): ValuePredicate? {
    if (language != PredicateService.LANGUAGE_PREDICATE) {
        error("Unsupported language: $language")
    }

    val predicate = getQuery(Predicate::class.java)
    return if (predicate is ValuePredicate && predicate.getAttribute() == RecordConstants.ATT_CREATED) {
        predicate
    } else {
        null
    }
}

private fun isAlfTask(id: String): Boolean {
    return id.startsWith(ALF_TASK_PREFIX)
}

fun RecordRef.isAlfTaskRef(): Boolean {
    return id.startsWith(ALF_TASK_PREFIX)
}

private data class FormInfo(
    val formId: String = "",
    val submitName: MLText = MLText()
)
