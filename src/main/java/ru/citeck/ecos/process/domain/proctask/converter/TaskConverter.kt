package ru.citeck.ecos.process.domain.proctask.converter

import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.task.IdentityLinkType
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.api.records.isAlfTask
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import javax.annotation.PostConstruct

private const val ALFRESCO_APP = "alfresco"
private const val AUTHORITY_SRC_ID = "authority"
private const val PEOPLE_SRC_ID = "people"

@Component
class TaskConverter(
    val camundaTaskService: TaskService,
    val recordsService: RecordsService
) {

    @PostConstruct
    private fun init() {
        cnv = this
    }
}

private lateinit var cnv: TaskConverter

fun Task.toProcTask(): ProcTaskDto {
    val links = cnv.camundaTaskService.getIdentityLinksForTask(id)
    val variables = cnv.camundaTaskService.getVariables(id)

    val candidateUsers = mutableSetOf<String>()
    val candidateGroups = mutableSetOf<String>()

    links.forEach {
        if (it.type == IdentityLinkType.CANDIDATE) {
            it.userId?.let { userId ->
                candidateUsers.add(userId)
            }
            it.groupId?.let { groupId ->
                candidateGroups.add(groupId)
            }
        }
    }

    return ProcTaskDto(
        id = id,
        name = MLText(name),
        priority = priority,
        formRef = RecordRef.valueOf(formKey),
        processInstanceId = if (processInstanceId.isNullOrBlank()) {
            RecordRef.EMPTY
        } else {
            RecordRef.create("eproc", BpmnProcRecords.ID, processInstanceId)
        },
        documentRef = if (variables[VAR_DOCUMENT_REF] != null) {
            RecordRef.valueOf(variables[VAR_DOCUMENT_REF].toString())
        } else {
            RecordRef.EMPTY
        },
        dueDate = dueDate,
        created = createTime,
        assignee = if (assignee.isNullOrBlank()) {
            RecordRef.EMPTY
        } else {
            createPeopleRef(assignee)
        },
        candidateUsers = candidateUsers.map { createPeopleRef(it) },
        candidateGroups = candidateGroups.map { createAuthorityRef(it) },
        definitionKey = taskDefinitionKey,
        variables = variables
    )
}

private fun createPeopleRef(userName: String): RecordRef {
    return RecordRef.create(ALFRESCO_APP, PEOPLE_SRC_ID, userName)
}

private fun createAuthorityRef(authorityId: String): RecordRef {
    return RecordRef.create(ALFRESCO_APP, AUTHORITY_SRC_ID, authorityId)
}

fun ProcTaskDto.toRecord(): ProcTaskRecords.ProcTaskRecord {
    return ProcTaskRecords.ProcTaskRecord(
        id = id,
        priority = priority,
        formRef = formRef,
        processInstanceId = processInstanceId,
        documentRef = documentRef,
        created = created,
        dueDate = dueDate,
        title = name.getClosestValue(),
        actors = getActors(assignee, candidateUsers + candidateGroups),
        documentAtts = let {
            if (documentRef == RecordRef.EMPTY || isAlfTask(id)) {
                return@let RecordAtts()
            }

            val requiredAtts = AttContext.getInnerAttsMap()
                .filter { it.key.startsWith(DOCUMENT_FIELD_PREFIX) }
                .map { it.key.removePrefix(DOCUMENT_FIELD_PREFIX) to it.value.removePrefix(DOCUMENT_FIELD_PREFIX) }
                .toMap()

            cnv.recordsService.getAtts(documentRef, requiredAtts)
        },
        variables = variables
    )
}

private fun getActors(assignee: RecordRef, candidates: List<RecordRef>): List<AuthorityDto> {
    val actorsRefs = if (RecordRef.isNotEmpty(assignee)) {
        listOf(assignee)
    } else {
        candidates
    }

    return cnv.recordsService.getAtts(actorsRefs, AuthorityDto::class.java)
}
