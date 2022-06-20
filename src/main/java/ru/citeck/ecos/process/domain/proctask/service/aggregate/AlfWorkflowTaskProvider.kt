package ru.citeck.ecos.process.domain.proctask.service.aggregate

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.proctask.api.records.AggregationProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.AggregateTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

@Component
class AlfWorkflowTaskProvider(
    private val recordsService: RecordsService
) {

    fun queryTasks(recsQuery: RecordsQuery): RecsQueryRes<AggregateTaskDto> {
        val alfQuery = recsQuery.copy(sourceId = "alfresco/")
        val resultFromAlf = recordsService.query(alfQuery, AggregateTaskDto::class.java)

        resultFromAlf.getRecords().forEach {
            it.aggregationRef = RecordRef.valueOf("eproc/${AggregationProcTaskRecords.ID}@${it.id}")
        }

        return resultFromAlf
    }

}

