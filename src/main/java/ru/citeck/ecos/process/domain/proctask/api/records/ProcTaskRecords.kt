package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.apache.commons.lang3.time.FastDateFormat
import org.camunda.bpm.engine.TaskService
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
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.proctask.service.currentUserIsTaskActor
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import java.time.Instant
import kotlin.system.measureTimeMillis

@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService,
    private val camundaTaskService: TaskService
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao, RecordMutateDao {

    companion object {
        private val log = KotlinLogging.logger {}

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

        private const val FORM_INFO_ATT = "_formInfo"
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        // TODO: check actor filter $CURRENT and filter task query

        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        val camundaCount: Long
        val camundaCountTime = measureTimeMillis {
            camundaCount = camundaTaskService.createTaskQuery()
                .or()
                .taskAssigneeIn(currentUser)
                .taskCandidateUser(currentUser)
                .taskCandidateGroupIn(currentAuthorities)
                .endOr()
                .orderByTaskCreateTime()
                .desc()
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
                .orderByTaskCreateTime()
                .desc()
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

            result = recordsId.map { records.getOrDefault(it, null) }
        }

        log.debug { "Get Camunda Tasks records atts: $resultTime ms" }

        return result
    }

    override fun mutate(record: LocalRecordAtts): String {
        val task = procTaskService.getTaskById(record.id) ?: throw IllegalArgumentException(
            "Task with id " + "${record.id} not found"
        )

        val mutateInfo = TaskMutateVariables(task, record)
        if (task.documentRef != RecordRef.EMPTY && mutateInfo.documentAtts.getAttributes().isNotEmpty()) {
            recordsService.mutate(mutateInfo.documentAtts)
        }

        log.debug { "Submit task ${record.id} form with variables: ${mutateInfo.taskVariables}" }

        procTaskService.completeTask(record.id, mutateInfo.taskVariables)

        return record.id
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

            val outcome = getTaskOutcome(task, record)
            taskVariables[outcome.outcomeId()] = outcome.value
            taskVariables[outcome.nameId()] = outcome.name.toString()

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

        private fun getTaskOutcome(task: ProcTaskDto, record: LocalRecordAtts): Outcome {
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

            if (outcome.isBlank()) throw IllegalStateException("Task outcome is mandatory for task completion")
            if (task.definitionKey.isNullOrBlank()) {
                throw IllegalStateException("Task DefinitionKey is mandatory for task completion")
            }

            return Outcome(task.definitionKey, outcome, formInfo.submitName)
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

fun RecordRef.isAlfTaskRef(): Boolean {
    return id.startsWith(ALF_TASK_PREFIX)
}

private data class FormInfo(
    val formId: String = "",
    val submitName: MLText = MLText()
)
