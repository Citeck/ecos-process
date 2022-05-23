package ru.citeck.ecos.process.domain.proctask.converter

import mu.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.task.IdentityLinkType
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskRecord
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
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
        formRef = RecordRef.valueOf(formKey),
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
        definitionKey = taskDefinitionKey
    )
}

private fun createPeopleRef(userName: String): RecordRef {
    return RecordRef.create(ALFRESCO_APP, PEOPLE_SRC_ID, userName)
}

private fun createAuthorityRef(authorityId: String): RecordRef {
    return RecordRef.create(ALFRESCO_APP, AUTHORITY_SRC_ID, authorityId)
}

fun ProcTaskDto.toRecord(): ProcTaskRecord {
    return ProcTaskRecord(
        id = id,
        formRef = formRef,
        created = created,
        dueDate = dueDate,
        title = name.getClosestValue(),
        actors = getActors(assignee, candidateUsers + candidateGroups)
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
