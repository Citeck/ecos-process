package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.history.HistoricTaskInstanceQuery
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.TASK_COMPLETED_BY
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Component
class ProcHistoricTaskRecords(
    private val camundaHistoryService: HistoryService,
    private val procHistoricTaskService: ProcHistoricTaskService,
) : AbstractRecordsDao(), RecordsQueryDao, RecordsAttsDao {

    companion object {
        const val ID = "proc-historic-task"
        const val ENDED_ATT = "ended"

        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        // TODO: check actor filter $CURRENT and filter task query

        val currentUser = AuthContext.getCurrentUser()

        val historicTasksCount: Long
        val historicTasksCountTime = measureTimeMillis {
            historicTasksCount = camundaHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals(TASK_COMPLETED_BY, currentUser)
                .filterByCreated(recsQuery)
                .count()
        }

        val historicTasks: List<RecordRef>
        val historicTasksTime = measureTimeMillis {
            historicTasks = camundaHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals(TASK_COMPLETED_BY, currentUser)
                .filterByCreated(recsQuery)
                .sortByQuery(recsQuery)
                .finished()
                .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems)
                .map {
                    RecordRef.create(AppName.EPROC, ID, it.id)
                }
        }

        log.debug { "Camunda historic task count: $historicTasksCountTime ms" }
        log.debug { "Camunda historic tasks: $historicTasksTime ms" }

        val result = RecsQueryRes<RecordRef>()

        result.setRecords(historicTasks)
        result.setTotalCount(historicTasksCount)
        result.setHasMore(historicTasksCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }

    override fun getRecordsAtts(recordsId: List<String>): List<ProcTaskRecord?> {
        if (recordsId.isEmpty()) {
            return emptyList()
        }

        val result: List<ProcTaskRecord?>
        val time = measureTimeMillis {
            result = procHistoricTaskService.getHistoricTasksByIds(recordsId).map {
                it?.toRecord()
            }
        }

        log.debug { "Get Camunda Historic Tasks records atts: $time ms" }

        return result
    }
}

fun HistoricTaskInstanceQuery.filterByCreated(recsQuery: RecordsQuery): HistoricTaskInstanceQuery {
    val createdAttPredicate = recsQuery.getAttCreatedValuePredicate() ?: return this

    return when (createdAttPredicate.getType()) {
        ValuePredicate.Type.GT -> {
            this.startedAfter(Date.from(createdAttPredicate.getValue().getAsInstant()))
        }

        ValuePredicate.Type.LE -> {
            this.startedBefore(Date.from(createdAttPredicate.getValue().getAsInstant()))
        }

        else -> {
            error(
                "Unsupported predicate type: ${createdAttPredicate.getType()} " +
                    "for attribute ${createdAttPredicate.getAttribute()}"
            )
        }
    }
}

fun HistoricTaskInstanceQuery.sortByQuery(recsQuery: RecordsQuery): HistoricTaskInstanceQuery {
    val sortByCreated = recsQuery.sortBy.firstOrNull {
        it.attribute == RecordConstants.ATT_CREATED || it.attribute == ProcHistoricTaskRecords.ENDED_ATT
    } ?: return this.orderByHistoricTaskInstanceEndTime()
            .desc()

    return when (sortByCreated.attribute) {
        RecordConstants.ATT_CREATED -> {
            if (sortByCreated.ascending) {
                this.orderByHistoricActivityInstanceStartTime().asc()
            } else {
                this.orderByHistoricActivityInstanceStartTime().desc()
            }
        }

        ProcHistoricTaskRecords.ENDED_ATT -> {
            if (sortByCreated.ascending) {
                this.orderByHistoricTaskInstanceEndTime().asc()
            } else {
                this.orderByHistoricTaskInstanceEndTime().desc()
            }
        }

        else -> error("Unsupported sort attribute: ${sortByCreated.attribute}")
    }
}