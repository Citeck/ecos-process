package ru.citeck.ecos.process.domain.proctask.service.aggregate

import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.AggregateTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

@Component
class ProcTaskAggregator(
    private val camundaTaskService: TaskService,
    private val alfWorkflowTaskProvider: AlfWorkflowTaskProvider
) {

    fun queryTasks(recsQuery: RecordsQuery): RecsQueryRes<RecordRef> {
        //TODO: check actor filter $CURRENT and filter task query

        val resultFromAlf = alfWorkflowTaskProvider.queryTasks(recsQuery)

        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        val camundaCount = camundaTaskService.createTaskQuery()
            .or()
            .taskAssigneeIn(currentUser)
            .taskCandidateUser(currentUser)
            .taskCandidateGroupIn(currentAuthorities)
            .endOr()
            .orderByTaskCreateTime()
            .desc()
            .count()

        val tasksFromCamunda = camundaTaskService.createTaskQuery()
            .or()
            .taskAssigneeIn(currentUser)
            .taskCandidateUser(currentUser)
            .taskCandidateGroupIn(currentAuthorities)
            .endOr()
            .orderByTaskCreateTime()
            .desc()
            .initializeFormKeys()
            .listPage(recsQuery.page.skipCount, recsQuery.page.maxItems)
            .map {
                AggregateTaskDto(
                    id = it.id,
                    aggregationRef = RecordRef.valueOf("eproc/aggregation-proc-task@${it.id}"),
                    createTime = it.createTime
                )
            }

        val result = RecsQueryRes<RecordRef>()
        val aggregatedRecords = let {
            return@let (resultFromAlf.getRecords() + tasksFromCamunda)
                .sortedByDescending { it.createTime }
                .map { it.aggregationRef }
        }

        val aggregateTotalCount = resultFromAlf.getTotalCount() + camundaCount

        result.setRecords(aggregatedRecords.take(recsQuery.page.maxItems))
        result.setTotalCount(resultFromAlf.getTotalCount() + camundaCount)
        result.setHasMore(aggregateTotalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }


}
