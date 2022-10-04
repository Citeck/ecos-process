package ru.citeck.ecos.process.domain.proctask.converter

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.task.IdentityLinkType
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_NAME_ML
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.api.records.isAlfTaskRef
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.webapp.api.authority.EcosAuthorityService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct
import kotlin.system.measureTimeMillis

@Component
class TaskConverter(
    val camundaTaskService: TaskService,
    val recordsService: RecordsService,
    val authorityService: EcosAuthorityService,
    val camundaHistoryService: HistoryService
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
    val localVariables = cnv.camundaTaskService.getVariablesLocal(id)

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
        name = let {
            val nameMl = Json.mapper.convert(localVariables[VAR_NAME_ML], MLText::class.java) ?: MLText(name)
            return@let if (nameMl == MLText.EMPTY) {
                MLText(name)
            } else {
                nameMl
            }
        },
        priority = priority,
        formRef = RecordRef.valueOf(formKey),
        processInstanceId = if (processInstanceId.isNullOrBlank()) {
            RecordRef.EMPTY
        } else {
            RecordRef.create(AppName.EPROC, BpmnProcRecords.ID, processInstanceId)
        },
        documentRef = if (variables[VAR_DOCUMENT_REF] != null) {
            RecordRef.valueOf(variables[VAR_DOCUMENT_REF].toString())
        } else {
            RecordRef.EMPTY
        },
        dueDate = dueDate?.toInstant(),
        created = createTime.toInstant(),
        assignee = cnv.authorityService.getAuthorityRef(assignee),
        candidateUsers = candidateUsers.map { cnv.authorityService.getAuthorityRef(it) },
        candidateGroups = candidateGroups.map { cnv.authorityService.getAuthorityRef(it) },
        definitionKey = taskDefinitionKey,
        variables = variables
    )
}

fun ProcTaskDto.toRecord(): ProcTaskRecord {
    val res: ProcTaskRecord
    val time = measureTimeMillis {
        res = ProcTaskRecord(
            id = id,
            priority = priority,
            formRef = formRef,
            processInstanceId = processInstanceId,
            documentRef = documentRef,
            created = created,
            dueDate = dueDate,
            title = name,
            actors = getActors(assignee, candidateUsers + candidateGroups),
            documentAtts = let {
                if (documentRef == RecordRef.EMPTY || RecordRef.valueOf(id).isAlfTaskRef()) {
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

    println("Task to record: $time ms")

    return res
}

fun HistoricTaskInstance.toRecord(): ProcTaskRecord {
    val camundaHistoryService = cnv.camundaHistoryService

    var nameMl = MLText(name)
    var documentRef = RecordRef.EMPTY

    camundaHistoryService.createHistoricVariableInstanceQuery()
        .taskIdIn(id)
        .variableNameIn(VAR_NAME_ML)
        .list()
        .forEach {
            if (it.name == VAR_NAME_ML) {
                nameMl = Json.mapper.convert(it.value, MLText::class.java) ?: MLText(name)
            }
        }

    camundaHistoryService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .variableNameIn(VAR_DOCUMENT_REF)
        .list()
        .forEach {
            if (it.name == VAR_DOCUMENT_REF) {
                documentRef = RecordRef.valueOf(it.value.toString())
            }
        }

    return ProcTaskRecord(
        id = id,
        priority = priority,
        title = nameMl,
        documentRef = documentRef,
        created = startTime.toInstant(),
        ended = endTime.toInstant(),
        durationInMillis = durationInMillis,
        dueDate = dueDate?.toInstant(),
        processInstanceId = if (processInstanceId.isNullOrBlank()) {
            RecordRef.EMPTY
        } else {
            RecordRef.create(AppName.EPROC, BpmnProcRecords.ID, processInstanceId)
        }
    )
}

private fun getActors(assignee: EntityRef, candidates: List<EntityRef>): List<AuthorityDto> {
    val actorsRefs = if (EntityRef.isNotEmpty(assignee)) {
        listOf(assignee)
    } else {
        candidates
    }
    return cnv.recordsService.getAtts(actorsRefs, AuthorityDto::class.java)
}
