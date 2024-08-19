package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBElement
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.delegate.DelegateTask
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.dto.TaskRole
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.bpmnla.dto.UserTaskLaInfo
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.concurrent.TimeUnit

@Component
class TaskDefinitionUtils(
    @Lazy
    val camundaRepoService: RepositoryService,
    val roleService: RoleService
) {

    companion object {
        private val log = KotlinLogging.logger {}
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

    fun getTaskPossibleOutcomes(delegateTask: DelegateTask): List<TaskOutcome> {
        val taskDefinition =
            taskDeployedCamundaDefCache.get(delegateTask.processDefinitionId to delegateTask.taskDefinitionKey).task
                ?: return emptyList()
        val outcomesValue = taskDefinition.otherAttributes[BPMN_PROP_OUTCOMES]
        if (outcomesValue.isNullOrBlank()) {
            return emptyList()
        }

        return Json.mapper.readList(outcomesValue, TaskOutcome::class.java)
    }

    fun getTaskRoles(delegateTask: DelegateTask): List<TaskRole> {
        val document = delegateTask.getDocumentRef()
        if (document == EntityRef.EMPTY) {
            return emptyList()
        }

        val defData =
            taskDeployedCamundaDefCache.get(delegateTask.processDefinitionId to delegateTask.taskDefinitionKey)
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

    fun getTaskTitle(delegateTask: DelegateTask): MLText {
        with(delegateTask) {
            val defaultName = MLText(name ?: id)
            val titleFromDef = let {
                val taskDefinition = taskDeployedCamundaDefCache.get(processDefinitionId to taskDefinitionKey).task
                    ?: return MLText.EMPTY
                val propNameMlValue = taskDefinition.otherAttributes[BPMN_PROP_NAME_ML]
                if (propNameMlValue.isNullOrBlank()) {
                    return MLText.EMPTY
                }

                return@let Json.mapper.read(taskDefinition.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java)
                    ?: error(
                        "Can't read name from task ${taskDefinition.id}"
                    )
            }
            return if (titleFromDef != MLText.EMPTY) titleFromDef else defaultName
        }
    }

    fun getUserTaskLaInfo(key: Pair<String, String>): UserTaskLaInfo {
        val taskDefinition = taskDeployedCamundaDefCache.get(key).task ?: return UserTaskLaInfo()

        return UserTaskLaInfo(
            laEnabled = taskDefinition.otherAttributes[BPMN_PROP_LA_ENABLED].toBoolean(),
            laNotificationType = taskDefinition.otherAttributes[BPMN_PROP_LA_NOTIFICATION_TYPE]?.let {
                NotificationType.valueOf(it)
            },
            laNotificationTemplate = EntityRef.valueOf(
                taskDefinition.otherAttributes[BPMN_PROP_LA_NOTIFICATION_TEMPLATE]
            ),
            laManualNotificationTemplateEnabled =
            taskDefinition.otherAttributes[BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE_ENABLED].toBoolean(),
            laManualNotificationTemplate = taskDefinition.otherAttributes[BPMN_PROP_LA_MANUAL_NOTIFICATION_TEMPLATE],
            laReportEnabled = taskDefinition.otherAttributes[BPMN_PROP_LA_REPORT_ENABLED].toBoolean(),
            laSuccessReportNotificationTemplate =
            taskDefinition.otherAttributes[BPMN_PROP_LA_SUCCESS_REPORT_NOTIFICATION_TEMPLATE]?.let {
                EntityRef.valueOf(it)
            },
            laErrorReportNotificationTemplate =
            taskDefinition.otherAttributes[BPMN_PROP_LA_ERROR_REPORT_NOTIFICATION_TEMPLATE]?.let {
                EntityRef.valueOf(it)
            }
        )
    }

    fun getUserTaskLaInfo(delegateTask: DelegateTask): UserTaskLaInfo {
        return getUserTaskLaInfo(delegateTask.processDefinitionId to delegateTask.taskDefinitionKey)
    }

    data class CachedTaskDefData(
        val docType: EntityRef = EntityRef.EMPTY,
        val task: TUserTask? = null
    )
}
