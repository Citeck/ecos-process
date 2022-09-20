package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.xml.bind.JAXBElement

private const val SRC_ID_GROUP = "authority-group"

@Component
class CamundaExtensions(
    val camundaRepoService: RepositoryService
) {

    @PostConstruct
    private fun init() {
        ext = this
    }

    internal val taskTitleCache: LoadingCache<Pair<String, String>, MLText> = CacheBuilder.newBuilder()
        .refreshAfterWrite(40, TimeUnit.MINUTES)
        .expireAfterWrite(50, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build(CacheLoader.from { key -> getTaskTitleFromDeployedCamundaDefinition(key) })

    private fun getTaskTitleFromDeployedCamundaDefinition(key: Pair<String, String>?): MLText {
        if (key == null) {
            return MLText.EMPTY
        }

        val processDefinitionId = key.first
        val taskDefinitionKey = key.second

        val modelText = camundaRepoService.getProcessModel(processDefinitionId).reader().readText()
        if (modelText.isBlank()) {
            error("Can't get process model for process definition id: $processDefinitionId")
        }

        val definition = BpmnXmlUtils.readFromString(modelText)
        val process = let {
            if (definition.rootElement.size > 1) {
                error("Root elements is more than one not supported.")
            }

            return@let definition.rootElement.first().value as TProcess
        }

        fun getUserTasksFromFlowElements(flowElements: List<JAXBElement<out TFlowElement>>): List<TUserTask> {
            return flowElements.map { flowElement ->
                when (flowElement.value) {
                    is TUserTask -> {
                        return@map listOf(flowElement.value as TUserTask)
                    }

                    is TSubProcess -> {
                        return@map getUserTasksFromFlowElements((flowElement.value as TSubProcess).flowElement)
                    }

                    else -> return@map emptyList()
                }
            }.flatten()
        }

        val currentTask = getUserTasksFromFlowElements(process.flowElement)
            .firstOrNull { it.id == taskDefinitionKey } ?: error(
            "Task with id $taskDefinitionKey not found on camunda definition $processDefinitionId"
        )

        return Json.mapper.read(currentTask.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java) ?: error(
            "Can't read name from task ${currentTask.id}"
        )
    }
}

private lateinit var ext: CamundaExtensions

fun DelegateExecution.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateExecution.getNotBlankDocumentRef(): RecordRef {
    val documentFromVar = getDocumentRef()
    if (RecordRef.isEmpty(documentFromVar)) error("Document Ref can't be empty")
    return documentFromVar
}

fun DelegateTask.getDocumentRef(): RecordRef {
    val documentVar = getVariable(VAR_DOCUMENT_REF) as String?
    return RecordRef.valueOf(documentVar)
}

fun DelegateTask.getOutcome(): Outcome {
    val outcomeValue = getVariable("${taskDefinitionKey}$OUTCOME_POSTFIX")?.toString()
        ?: return Outcome.EMPTY

    val outcomeName = Json.mapper.convert(
        getVariable("${taskDefinitionKey}$OUTCOME_NAME_POSTFIX")?.toString() ?: "",
        MLText::class.java
    ) ?: MLText()

    return Outcome(taskDefinitionKey, outcomeValue, outcomeName)
}

fun DelegateTask.getTitle(): MLText {
    val defaultName = MLText(name)

    val processDefIdToTaskKey = Pair(processDefinitionId, taskDefinitionKey)
    return ext.taskTitleCache.get(processDefIdToTaskKey) ?: defaultName
}

fun EntityRef.isAuthorityGroupRef(): Boolean {
    return getSourceId() == SRC_ID_GROUP
}

fun TaskPriority.toCamundaCode(): Int {
    return when (this) {
        TaskPriority.LOW -> 3
        TaskPriority.MEDIUM -> 2
        TaskPriority.HIGH -> 1
    }
}

fun ActivityImpl.addTaskListener(eventName: String, listener: TaskListener) {
    val activityBehavior = activityBehavior
    if (activityBehavior is UserTaskActivityBehavior) {
        activityBehavior.taskDefinition.addTaskListener(
            eventName,
            listener
        )
    }
}
