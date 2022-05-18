package ru.citeck.ecos.process.domain.proctask.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.io.convert.fullId
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_PREFIX
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao

@Component
class ProcTaskRecords(
    private val procTaskService: ProcTaskService
) : AbstractRecordsDao(), RecordAttsDao, RecordMutateDao {

    companion object {
        const val ID = "proc-task"
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

        val taskOutcome = getTaskOutcome(task, record)

        val variables = mutableMapOf<String, Any>()
        variables[taskOutcome.fullId()] = taskOutcome.value

        procTaskService.submitTaskForm(record.id, variables)

        return record.id
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

}
