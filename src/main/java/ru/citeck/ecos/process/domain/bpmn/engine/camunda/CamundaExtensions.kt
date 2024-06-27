package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.task.IdentityLink
import org.camunda.bpm.engine.task.IdentityLinkType
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_NAME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_POSTFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SRC_ID_GROUP = "authority-group"

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
