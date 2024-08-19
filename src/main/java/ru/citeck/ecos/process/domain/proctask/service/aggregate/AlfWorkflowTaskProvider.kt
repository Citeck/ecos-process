package ru.citeck.ecos.process.domain.proctask.service.aggregate

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.AggregateTaskDto
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class AlfWorkflowTaskProvider(
    private val recordsService: RecordsService
) {

    fun queryTasks(recsQuery: RecordsQuery): RecsQueryRes<AggregateTaskDto> {
        val alfQuery = recsQuery.copy(sourceId = "alfresco/")
        val resultFromAlf = recordsService.query(alfQuery, AggregateTaskDto::class.java)

        resultFromAlf.getRecords().forEach {
            it.aggregationRef = EntityRef.valueOf("${AppName.EPROC}/${ProcTaskRecords.ID}@${it.id}")
        }

        return resultFromAlf
    }
}
