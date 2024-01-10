package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import ecos.guava30.com.google.common.cache.CacheBuilder
import ecos.guava30.com.google.common.cache.CacheLoader
import ecos.guava30.com.google.common.cache.LoadingCache
import mu.KotlinLogging
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.task.IdentityLink
import org.camunda.bpm.engine.task.IdentityLinkType
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.TaskRole
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ASSIGNEES
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_OUTCOMES
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.xml.bind.JAXBElement

private const val SRC_ID_GROUP = "authority-group"

@Component
class CamundaExtensions(
    val camundaRepoService: RepositoryService,
    val roleService: RoleService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @PostConstruct
    private fun init() {
        ext = this
    }

    private val taskDeployedCamundaDefCache: LoadingCache<Pair<String, String>, CachedTaskDefData> =
        CacheBuilder.newBuilder()
            .refreshAfterWrite(40, TimeUnit.MINUTES)
            .expireAfterWrite(50, TimeUnit.MINUTES)
            .maximumSize(300)
            .build(CacheLoader.from { key -> getUserTaskDefinition(key) })

    private fun getUserTaskDefinition(key: Pair<String, String>?): CachedTaskDefData {
        if (key == null) {
            return CachedTaskDefData()
        }

        val processDefinitionId = key.first
        val taskDefinitionKey = key.second

        log.debug { "Getting task title with procDefId: $processDefinitionId, taskDefKey: $taskDefinitionKey" }

        val modelText = camundaRepoService.getProcessModel(processDefinitionId).reader().readText()
        if (modelText.isBlank()) {
            error("Can't get process model for process definition id: $processDefinitionId")
        }

        val definition = BpmnXmlUtils.readFromString(modelText)
        val allFlowElements = definition.rootElement.map {
            val element = it.value
            if (element is TProcess) {
                element.flowElement
            } else {
                emptyList()
            }
        }.flatten()

        fun getUserTasksFromFlowElements(flowElements: List<JAXBElement<out TFlowElement>>): List<TUserTask> {
            return flowElements.map { flowElement ->
                when (val element = flowElement.value) {
                    is TUserTask -> {
                        return@map listOf(element)
                    }

                    is TSubProcess -> {
                        return@map getUserTasksFromFlowElements(element.flowElement)
                    }

                    else -> return@map emptyList()
                }
            }.flatten()
        }

        val currentTask = getUserTasksFromFlowElements(allFlowElements)
            .firstOrNull { it.id == taskDefinitionKey } ?: error(
            "Task with id $taskDefinitionKey not found on camunda definition $processDefinitionId"
        )

        val type = EntityRef.valueOf(definition.otherAttributes[BPMN_PROP_ECOS_TYPE] ?: "")

        log.debug { "Found current task: \n$currentTask \necos type:$type " }

        return CachedTaskDefData(type, currentTask)
    }

    internal fun getTaskTitle(key: Pair<String, String>): MLText {
        val taskDefinition = taskDeployedCamundaDefCache.get(key).task ?: return MLText.EMPTY
        val propNameMlValue = taskDefinition.otherAttributes[BPMN_PROP_NAME_ML]
        if (propNameMlValue.isNullOrBlank()) {
            return MLText.EMPTY
        }

        return Json.mapper.read(taskDefinition.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java) ?: error(
            "Can't read name from task ${taskDefinition.id}"
        )
    }

    internal fun getTaskPossibleOutcomes(key: Pair<String, String>): List<TaskOutcome> {
        val taskDefinition = taskDeployedCamundaDefCache.get(key).task ?: return emptyList()
        val outcomesValue = taskDefinition.otherAttributes[BPMN_PROP_OUTCOMES]
        if (outcomesValue.isNullOrBlank()) {
            return emptyList()
        }

        return Json.mapper.readList(outcomesValue, TaskOutcome::class.java)
    }

    internal fun getTaskRoles(document: EntityRef, key: Pair<String, String>): List<TaskRole> {
        if (document == RecordRef.EMPTY) {
            return emptyList()
        }

        val defData = taskDeployedCamundaDefCache.get(key)
        val taskDefinition = defData.task ?: return emptyList()

        val roleData = taskDefinition.otherAttributes[BPMN_PROP_ASSIGNEES]
        if (roleData.isNullOrBlank()) {
            return emptyList()
        }

        val rolesFromDef = Json.mapper.readList(roleData, String::class.java)
        if (rolesFromDef.isEmpty()) {
            return emptyList()
        }

        val currentUserRoles = roleService.getCurrentUserRoles(document, defData.docType)

        return currentUserRoles.intersect(rolesFromDef.toSet()).map { role ->
            TaskRole(
                role,
                roleService.getRoleDef(defData.docType, role).name
            )
        }
    }

    data class CachedTaskDefData(
        val docType: EntityRef = EntityRef.EMPTY,
        val task: TUserTask? = null
    )
}

private lateinit var ext: CamundaExtensions

fun DelegateExecution.getDocumentRef(): EntityRef {
    val documentVar = getVariable(BPMN_DOCUMENT_REF) as String?
    return EntityRef.valueOf(documentVar)
}

fun DelegateExecution.getNotBlankDocumentRef(): EntityRef {
    val documentFromVar = getDocumentRef()
    if (EntityRef.isEmpty(documentFromVar)) error("Document Ref can't be empty")
    return documentFromVar
}

fun DelegateTask.getDocumentRef(): EntityRef {
    val documentVar = getVariable(BPMN_DOCUMENT_REF) as String?
    return EntityRef.valueOf(documentVar)
}

fun DelegateTask.getFormRef(): EntityRef {
    return if (this is TaskEntity) {
        val taskDef = this.taskDefinition
        val formKey = taskDef?.formKey?.expressionText ?: ""

        EntityRef.valueOf(formKey)
    } else {
        EntityRef.EMPTY
    }
}

fun DelegateTask.getProcessInstanceRef(): EntityRef {
    return if (processInstanceId.isNotBlank()) {
        EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, processInstanceId)
    } else {
        EntityRef.EMPTY
    }
}

fun DelegateExecution.getProcessInstanceRef(): EntityRef {
    return if (processInstanceId.isNotBlank()) {
        EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, processInstanceId)
    } else {
        EntityRef.EMPTY
    }
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

fun DelegateTask.getPossibleOutcomes(): List<TaskOutcome> {
    return ext.getTaskPossibleOutcomes(processDefinitionId to taskDefinitionKey)
}

fun DelegateTask.getTaskRoles(): List<TaskRole> {
    return ext.getTaskRoles(getDocumentRef(), processDefinitionId to taskDefinitionKey)
}

fun DelegateTask.getTitle(): MLText {
    val defaultName = MLText(name ?: id)
    val titleFromDef = ext.getTaskTitle(processDefinitionId to taskDefinitionKey)
    return if (titleFromDef != MLText.EMPTY) titleFromDef else defaultName
}

fun DelegateTask.getCompletedBy(): String {
    return getVariable(BPMN_TASK_COMPLETED_BY) as String? ?: ""
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

fun Collection<IdentityLink>.splitToUserGroupCandidates(): Pair<Set<String>, Set<String>> {
    if (this.isEmpty()) {
        return Pair(emptySet(), emptySet())
    }

    val candidateUsers = mutableSetOf<String>()
    val candidateGroups = mutableSetOf<String>()

    this.forEach {
        if (it.type == IdentityLinkType.CANDIDATE) {
            it.userId?.let { userId ->
                candidateUsers.add(userId)
            }
            it.groupId?.let { groupId ->
                candidateGroups.add(groupId)
            }
        }
    }

    return Pair(candidateUsers, candidateGroups)
}
