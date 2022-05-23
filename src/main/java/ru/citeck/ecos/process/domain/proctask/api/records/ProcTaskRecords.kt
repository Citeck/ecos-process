package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.io.convert.fullId
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.proctask.service.currentUserIsTaskActor
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao

@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService
) : AbstractRecordsDao(), RecordAttsDao, RecordMutateDao {

    companion object {
        const val ID = "proc-task"

        private const val DOCUMENT_FIELD_PREFIX = "_ECM_"
    }

    override fun getId(): String {
        return ID
    }

    override fun getRecordAtts(recordId: String): ProcTaskRecord? {
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

        procTaskService.submitTaskForm(record.id, mutateInfo.taskVariables)

        return record.id
    }

    inner class TaskMutateVariables(
        private val task: ProcTaskDto,
        record: LocalRecordAtts
    ) {
        val taskVariables = mutableMapOf<String, Any?>()
        val documentAtts = RecordAtts()

        init {
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
                        //do nothing
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
