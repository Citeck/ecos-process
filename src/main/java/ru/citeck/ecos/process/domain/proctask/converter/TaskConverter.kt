package ru.citeck.ecos.process.domain.proctask.converter

import mu.KotlinLogging
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.camunda.bpm.engine.task.Task
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.api.records.isAlfTaskRef
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext

internal val log = KotlinLogging.logger {}

@Component
class TaskConverter(
    private val cacheableTaskConverter: CacheableTaskConverter,
    private val recordsService: RecordsService,
    @Lazy
    private val procTaskRecords: ProcTaskRecords
) {

    fun toProcTask(task: Task): ProcTaskDto {
        log.trace { "toProcTask: task=${task.id}" }

        val dto = cacheableTaskConverter.convertTask(task)

        log.trace { "Task $${task.id} converted to $dto" }

        return dto
    }

    fun toRecord(procTaskDto: ProcTaskDto): ProcTaskRecords.ProcTaskRecord {
        with(procTaskDto) {
            return procTaskRecords.ProcTaskRecord(
                id = id,
                priority = priority,
                formRef = formRef,
                processInstanceRef = processInstanceId,
                documentRef = documentRef,
                documentType = documentType,
                documentTypeRef = documentTypeRef,
                created = created,
                ended = ended,
                durationInMillis = durationInMillis,
                dueDate = dueDate,
                followUpDate = followUpDate,
                title = name,
                assignee = assignee,
                owner = owner,
                senderTemp = sender,
                candidateUsers = candidateUsers,
                candidateGroups = candidateGroups,
                possibleOutcomes = possibleOutcomes,
                comment = comment,
                lastComment = lastComment,
                definitionKey = definitionKey,
                documentAtts = let {
                    if (documentRef == RecordRef.EMPTY || RecordRef.valueOf(id).isAlfTaskRef()) {
                        return@let RecordAtts()
                    }

                    val requiredAtts = AttContext.getInnerAttsMap()
                        .filter { it.key.startsWith(DOCUMENT_FIELD_PREFIX) }
                        .map { it.key.removePrefix(DOCUMENT_FIELD_PREFIX) to it.value.removePrefix(DOCUMENT_FIELD_PREFIX) }
                        .toMap()

                    recordsService.getAtts(documentRef, requiredAtts)
                },
                historic = historic,
                engineAtts = engineAtts,
                reassignable = assignee.isNotEmpty(),
                claimable = assignee.isEmpty() && (candidateUsers.isNotEmpty() || candidateGroups.isNotEmpty()),
                unclaimable = assignee.isNotEmpty() &&
                    (candidateUsersOriginal.isNotEmpty() || candidateGroupsOriginal.isNotEmpty()),
                assignable = assignee.isEmpty() && (candidateUsers.isNotEmpty() || candidateGroups.isNotEmpty()),
            )
        }
    }

    fun toProcTask(historicTaskInstance: HistoricTaskInstance): ProcTaskDto {
        return cacheableTaskConverter.convertHistoricTask(historicTaskInstance)
    }
}
