package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.apache.commons.lang3.time.FastDateFormat
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.task.TaskQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_PREFIX
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords.Companion.ALF_TASK_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.*
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import java.time.Instant
import java.util.*
import kotlin.system.measureTimeMillis

const val CHANGE_OWNER_RECORD_ACTION_CLAIM = "claim"
const val CHANGE_OWNER_RECORD_ACTION_RELEASE = "release"

const val CHANGE_OWNER_ATT = "changeOwner"
const val CHANGE_OWNER_ACTION_ATT = "action"
const val CHANGE_OWNER_USER_ATT = "owner"



@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService,
    private val camundaTaskService: TaskService,
    private val procTaskOwnership: ProcTaskOwnership
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
        // TODO: check actor filter $CURRENT and filter task query

        val fullAuth = AuthContext.getCurrentFullAuth()

        val currentUser = fullAuth.getUser()
        val currentAuthorities = let {
            mutableSetOf(currentUser).apply {
                addAll(fullAuth.getAuthorities())
            }.toList()
        }

        val camundaCount: Long
        val camundaCountTime = measureTimeMillis {
            camundaCount = camundaTaskService.createTaskQuery()
                .or()
                .taskAssigneeIn(currentUser)
                .taskCandidateUser(currentUser)
                .taskCandidateGroupIn(currentAuthorities)
                .endOr()
                .filterByCreated(recsQuery)
                .count()
        }

        val tasksFromCamunda: List<RecordRef>
        val tasksFromCamundaTime = measureTimeMillis {
            tasksFromCamunda = camundaTaskService.createTaskQuery()
                .or()
                .taskAssigneeIn(currentUser)
                .taskCandidateUser(currentUser)
                .taskCandidateGroupIn(currentAuthorities)
                .endOr()
                .filterByCreated(recsQuery)
                .sortByQuery(recsQuery)
                .initializeFormKeys()
                .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems)
                .map {
                    RecordRef.create(AppName.EPROC, ID, it.id)
                }
        }

        log.debug { "Camunda task count: $camundaCountTime ms" }
        log.debug { "Camunda tasks: $tasksFromCamundaTime ms" }

        val result = RecsQueryRes<RecordRef>()

        result.setRecords(tasksFromCamunda)
        result.setTotalCount(camundaCount)
        result.setHasMore(camundaCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordsId: List<String>): List<ProcTaskRecord?>? {
        if (recordsId.isEmpty()) {
            return emptyList()
        }

        val result: List<ProcTaskRecord?>
        val resultTime = measureTimeMillis {
            val records = mutableMapOf<String, ProcTaskRecord?>()

            val procRefs = mutableListOf<String>()

            recordsId.forEach {
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
                    record = it?.toRecord()
                }

                log.trace { "To record: $toRecordTime ms" }

                record?.let { rec ->
                    records[rec.id] = rec
                }
            }

            result = recordsId.map { records[it] }
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

        procTaskService.completeTask(record.id, outcome, mutateInfo.taskVariables)

        return record.id
    }

    private fun LocalRecordAtts.isChangeOwnerAction(): Boolean {
        return getAtt(CHANGE_OWNER_ATT).isNotEmpty()
    }

    private fun LocalRecordAtts.toTaskOwnershipChangeData(taskId: String): TaskOwnershipChangeData {
        val changeOwner = getAtt(CHANGE_OWNER_ATT)
        val actionData = changeOwner.get(CHANGE_OWNER_ACTION_ATT).asText()
        val userData = changeOwner.get(CHANGE_OWNER_USER_ATT).asText()

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
            userData
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
            if (!currentUserIsTaskActor(task)) {
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
}

fun TaskQuery.filterByCreated(recsQuery: RecordsQuery): TaskQuery {
    val createdAttPredicate = recsQuery.getAttCreatedValuePredicate() ?: return this

    return when (createdAttPredicate.getType()) {
        ValuePredicate.Type.GT -> {
            this.taskCreatedAfter(Date.from(createdAttPredicate.getValue().getAsInstant()))
        }

        ValuePredicate.Type.LE -> {
            this.taskCreatedBefore(Date.from(createdAttPredicate.getValue().getAsInstant()))
        }

        else -> {
            error(
                "Unsupported predicate type: ${createdAttPredicate.getType()} " +
                    "for attribute ${createdAttPredicate.getAttribute()}"
            )
        }
    }
}

fun TaskQuery.sortByQuery(recsQuery: RecordsQuery): TaskQuery {
    val sortByCreated = recsQuery.getSortByCreated()
        ?: return this.orderByTaskCreateTime()
            .desc()

    if (sortByCreated.attribute != RecordConstants.ATT_CREATED) {
        error("Unsupported sort attribute: ${sortByCreated.attribute}")
    }

    return if (sortByCreated.ascending) {
        this.orderByTaskCreateTime().asc()
    } else {
        this.orderByTaskCreateTime().desc()
    }
}

fun RecordsQuery.getSortByCreated(): SortBy? {
    return this.sortBy.firstOrNull {
        it.attribute == RecordConstants.ATT_CREATED
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

fun RecordRef.isAlfTaskRef(): Boolean {
    return id.startsWith(ALF_TASK_PREFIX)
}

private data class FormInfo(
    val formId: String = "",
    val submitName: MLText = MLText()
)
