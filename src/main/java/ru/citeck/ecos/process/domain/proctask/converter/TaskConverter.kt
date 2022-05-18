package ru.citeck.ecos.process.domain.proctask.converter

import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.task.IdentityLinkType
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
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
        dueDate = dueDate,
        assignee = let {
            if (assignee.isNullOrBlank()) {
                null
            } else {
                createPeopleRef(assignee)
            }
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
        dueDate = dueDate,
        title = name.getClosestValue(),
        actors = getActorsFromCandidates(candidateUsers + candidateGroups)
    )
}

private fun getActorsFromCandidates(candidates: List<RecordRef>): List<AuthorityDto> {
    return cnv.recordsService.getAtts(candidates, AuthorityDto::class.java)
}
