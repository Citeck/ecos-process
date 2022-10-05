package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.converter.toRecord
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.process.domain.proctask.service.TASK_COMPLETED_BY
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
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
        private val log = KotlinLogging.logger {}

        const val ID = "proc-historic-task"
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
                .count()
        }

        val historicTasks: List<RecordRef>
        val historicTasksTime = measureTimeMillis {
            historicTasks = camundaHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals(TASK_COMPLETED_BY, currentUser)
                .orderByHistoricTaskInstanceEndTime()
                .desc()
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
