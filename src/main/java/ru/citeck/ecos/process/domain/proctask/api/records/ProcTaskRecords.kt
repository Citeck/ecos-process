package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.apache.commons.lang3.time.FastDateFormat
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.bpmn.COMMENT_VAR
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.io.convert.fullId
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.proctask.service.aggregate.ProcTaskAggregator
import ru.citeck.ecos.process.domain.proctask.service.currentUserIsTaskActor
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.time.Instant

@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService,
    private val procTaskAggregator: ProcTaskAggregator
) : AbstractRecordsDao(), RecordsQueryDao, RecordAttsDao, RecordMutateDao {

    companion object {
        private val log = KotlinLogging.logger {}

        const val ID = "proc-task"

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
        return procTaskAggregator.queryTasks(recsQuery)
    }

    override fun getRecordAtts(recordId: String): ProcTaskRecord? {
        if (recordId.isBlank()) {
            return null
        }

        if (isAlfTask(recordId)) {
            return createTaskRecordFromAlf(recordId)
        }

        val task = procTaskService.getTaskById(recordId) ?: return null
        return task.toRecord()
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

        procTaskService.submitTaskForm(record.id, mutateInfo.taskVariables)

        return record.id
    }

    class ProcTaskRecord(
        val id: String,
        val priority: Int = 0,
        val formRef: RecordRef? = null,
        val processInstanceId: RecordRef? = null,
        val documentRef: RecordRef? = null,
        val title: String? = null,
        val created: Instant? = null,
        val dueDate: Instant? = null,
        val actors: List<AuthorityDto> = emptyList(),

        val documentAtts: RecordAtts = RecordAtts(),
        val variables: Map<String, Any> = emptyMap(),

        val alfTaskAtts: RecordAtts = RecordAtts()
    ) {

        // TODO: add default form. simple-form is default?
        @get:AttName("_formRef")
        val formKey: RecordRef
            get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

        @AttName("started")
        fun getStarted(): Instant? {
            return created
        }

        @AttName(".disp")
        fun getDisp(): String? {
            return title
        }

        @AttName("name")
        fun getName(): String? {
            return title
        }

        @AttName("workflow")
        fun getWorkflow(): RecordRef? {
            return processInstanceId
        }

        fun getAtt(name: String): Any? {
            val mapping = if (isAlfTask(id)) EPROC_TO_ALF_TASK_ATTS else ALF_TO_ERPOC_TASK_ATTS

            if (isAlfTask(id)) {
                if (mapping.containsKey(name)) {
                    val fixedAttName = mapping[name]
                    return alfTaskAtts.getAtt(fixedAttName)
                }

                return alfTaskAtts.getAtt(name)
            }

            if (name == COMMENT_VAR) {
                return null
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

            return variables[name]
        }
    }

    private fun createTaskRecordFromAlf(recordId: String): ProcTaskRecord {
        val mapping = EPROC_TO_ALF_TASK_ATTS
        val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss")

        val alfAtts = let {
            if (!isAlfTask(recordId)) {
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

            val fullOriginalRef = RecordRef.create("alfresco", "", recordId)

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
            id = recordId,
            documentRef = RecordRef.valueOf(alfAtts.getAtt(mapping["documentRef"]).asText()),
            dueDate = getDateFromAtts("dueDate"),
            priority = alfAtts.getAtt(mapping["priority"]).asInt(),
            created = getDateFromAtts("created"),
            alfTaskAtts = alfAtts,
            title = alfAtts.getAtt(mapping["disp"]).asText(),
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
            taskVariables[outcome.fullId()] = outcome.value

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
            val outcome = let {
                var outcomeAttValue = ""

                record.forEach { k, v ->
                    if (k.startsWith(OUTCOME_PREFIX) && v.asBoolean()) {
                        outcomeAttValue = k
                    }
                }

                outcomeAttValue.substringAfter(OUTCOME_PREFIX)
            }

            if (outcome.isBlank()) throw IllegalStateException("Task outcome is mandatory for task completion")
            if (task.definitionKey.isNullOrBlank()) {
                throw IllegalStateException("Task DefinitionKey is mandatory for task completion")
            }

            return Outcome(task.definitionKey, outcome)
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

fun isAlfTask(id: String): Boolean {
    return id.startsWith("workspace")
}
