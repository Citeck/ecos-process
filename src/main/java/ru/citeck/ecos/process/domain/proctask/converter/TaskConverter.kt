package ru.citeck.ecos.process.domain.proctask.converter

import mu.KotlinLogging
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.task.Task
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.api.records.isAlfTaskRef
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import javax.annotation.PostConstruct

internal val log = KotlinLogging.logger {}

@Component
class TaskConverter(
    val cacheableTaskConverter: CacheableTaskConverter,
    val recordsService: RecordsService
) {

    @PostConstruct
    private fun init() {
        cnv = this
    }
}

private lateinit var cnv: TaskConverter

fun Task.toProcTask(): ProcTaskDto {
    log.trace { "toProcTask: task=$id" }

    val dto = cnv.cacheableTaskConverter.convertTask(this)

    log.trace { "Task $id converted to $dto" }

    return dto
}

fun ProcTaskDto.toRecord(): ProcTaskRecord {
    return ProcTaskRecord(
        id = id,
        priority = priority,
        formRef = formRef,
        processInstanceId = processInstanceId,
        documentRef = documentRef,
        created = created,
        ended = ended,
        durationInMillis = durationInMillis,
        dueDate = dueDate,
        followUpDate = followUpDate,
        title = name,
        assignee = assignee,
        senderTemp = sender,
        candidateUsers = candidateUsers,
        candidateGroups = candidateGroups,
        possibleOutcomes = possibleOutcomes,
        definitionKey = definitionKey,
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
        historic = historic,
        engineAtts = engineAtts
    )
}

fun HistoricTaskInstance.toProcTask(): ProcTaskDto {
    return cnv.cacheableTaskConverter.convertHistoricTask(this)
}
