package ru.citeck.ecos.process.domain.proctask.service

import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

/**
 * @author Roman Makarskiy
 */
interface ProcHistoricTaskService {

    fun getHistoricTaskById(taskId: String): ProcTaskDto?

    fun getHistoricTasksByIds(ids: List<String>): List<ProcTaskDto?>
}
