package ru.citeck.ecos.process.domain.proctask.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Service
class ProcHistoricTaskServiceImpl(
    private val historicTaskLoader: HistoricTaskLoader,
    private val taskConverter: TaskConverter
) : ProcHistoricTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getHistoricTaskById(taskId: String): ProcTaskDto? {
        val historicTask = historicTaskLoader.tasks(listOf(taskId)).firstOrNull()
        return historicTask?.let { taskConverter.toProcTask(it) }
    }

    override fun getHistoricTasksByIds(ids: List<String>): List<ProcTaskDto?> {
        log.trace { "Get Historic Camunda Tasks by ids: $ids" }

        val tasks: Map<String, ProcTaskDto>
        val getTasksTime = measureTimeMillis {
            tasks = historicTaskLoader.tasks(ids)
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
