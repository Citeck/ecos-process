package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.apache.commons.lang3.LocaleUtils
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.proctask.dto.CompleteTaskData
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService

@Component("tasks")
class CamundaProcessTaskService(
    @Lazy
    private val procTaskService: ProcTaskService
) : CamundaProcessEngineService {

    companion object {
        private val defaultDoneOutcome = TaskOutcome(
            "defaultDone",
            MLText(
                mapOf(
                    LocaleUtils.toLocale("en") to "Done",
                    LocaleUtils.toLocale("ru") to "Выполнено"
                )
            )
        )
    }

    override fun getKey(): String {
        return "tasks"
    }

    /**
     * Complete all active tasks for [DelegateExecution.getProcessInstanceId] with default outcome [defaultDoneOutcome].
     * If bpmn task has outcome with id [defaultDoneOutcome.id] then name defined in bpmn task will be used.
     */
    fun completeActiveTasks(execution: DelegateExecution) {
        val processInstanceId = execution.processInstanceId

        procTaskService.getTasksByProcess(processInstanceId)
            .filter { !it.isDeleted }
            .forEach { task ->
                if (task.definitionKey.isNullOrBlank()) {
                    error(
                        "Failed to complete task ${task.id} in ${execution.processInstanceId}, " +
                            "because Task DefinitionKey is blank"
                    )
                }
                val defaultOutcomeNameFromTask = task.possibleOutcomes.find { it.id == defaultDoneOutcome.id }?.name

                val outcome = Outcome(
                    taskDefinitionKey = task.definitionKey,
                    value = defaultDoneOutcome.id,
                    name = defaultOutcomeNameFromTask ?: defaultDoneOutcome.name
                )

                procTaskService.completeTask(CompleteTaskData(task, outcome, emptyMap()))
            }
    }
}
