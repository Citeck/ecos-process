package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.HistoryService
import org.springframework.stereotype.Service
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaMyBatisExtension
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.getHistoricTasksByIds
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Service
class ProcHistoricTaskServiceImpl(
    private val camundaHistoryService: HistoryService,
    private val camundaMyBatisExtension: CamundaMyBatisExtension,
    private val taskConverter: TaskConverter
) : ProcHistoricTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getHistoricTaskById(taskId: String): ProcTaskDto? {
        val historicTask = camundaHistoryService.createHistoricTaskInstanceQuery()
            .taskId(taskId)
            .singleResult()
        return historicTask?.let { taskConverter.toProcTask(it) }
    }

    override fun getHistoricTasksByIds(ids: List<String>): List<ProcTaskDto?> {
        log.trace { "Get Historic Camunda Tasks by ids: $ids" }

        val tasks: Map<String, ProcTaskDto>
        val getTasksTime = measureTimeMillis {
            tasks = camundaHistoryService.getHistoricTasksByIds(ids, camundaMyBatisExtension)
                .associate { it.id to taskConverter.toProcTask(it) }
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
