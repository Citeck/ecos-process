package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.springframework.stereotype.Service
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getHistoricTasksByIds
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Service
class ProcHistoricTaskServiceImpl(
    private val camundaHistoryService: HistoryService
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
        log.trace { "Get Historic Camunda Tasks by ids: $ids" }

        val tasks: Map<String, ProcTaskDto>
        val getTasksTime = measureTimeMillis {
            tasks = camundaHistoryService.getHistoricTasksByIds(ids).associate { it.id to it.toProcTask() }
        }

        val result = mutableListOf<ProcTaskDto?>()
        val resultTime = measureTimeMillis {
            ids.forEach { id ->
                result.add(tasks[id])
            }
        }

        log.trace { "Get Historic Camunda Tasks by ids return: $result" }
        log.debug {
            "Get Historic Camunda Tasks by ids: $getTasksTime ms, mapping: $resultTime, " +
                "total: ${getTasksTime + resultTime} ms"
        }

        return result
    }
}
