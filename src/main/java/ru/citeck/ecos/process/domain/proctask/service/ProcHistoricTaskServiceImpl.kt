package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.history.HistoricTaskInstance
import org.springframework.stereotype.Service
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Service
class ProcHistoricTaskServiceImpl(
    private val camundaHistoryService: HistoryService,
    private val managementService: ManagementService
) : ProcHistoricTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getHistoricTaskById(taskId: String): ProcTaskDto? {
        return camundaHistoryService.createHistoricTaskInstanceQuery()
            .taskId(taskId)
            .singleResult()
            ?.toProcTask()
    }

    override fun getHistoricTasksByIds(ids: List<String>): List<ProcTaskDto?> {
        val result: List<ProcTaskDto?>
        val time = measureTimeMillis {
            result = camundaHistoryService.createNativeHistoricTaskInstanceQuery()
                .sql(
                    "select * from ${managementService.getTableName(HistoricTaskInstance::class.java)} " +
                        "where ID_ in ('${ids.joinToString("','")}')"
                )
                .list()
                .map { it?.toProcTask() }
        }

        log.debug { "Get Historic Camunda Tasks by ids: $time ms" }

        return result
    }
}
