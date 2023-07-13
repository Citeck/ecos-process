package ru.citeck.ecos.process.domain.proctask.service.aggregate

import mu.KotlinLogging
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.AggregateTaskDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName

// TODO: Remove aggregation from alfresco?
@Component
class ProcTaskAggregator(
    private val camundaTaskService: TaskService,
    private val alfWorkflowTaskProvider: AlfWorkflowTaskProvider
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun queryTasks(recsQuery: RecordsQuery): RecsQueryRes<RecordRef> {
        // TODO: check actor filter $CURRENT and filter task query

        val originalSkip = recsQuery.page.skipCount
        val fixedQuery = recsQuery.copy {
            withSkipCount(0)
            withMaxItems(originalSkip + recsQuery.page.maxItems)
        }

        val resultFromAlf = alfWorkflowTaskProvider.queryTasks(fixedQuery)

        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities()

        log.debug {
            "queryTasks: user=$currentUser userAuthorities=$currentAuthorities"
        }

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
            .listPage(0, originalSkip + recsQuery.page.maxItems)
            .map {
                AggregateTaskDto(
                    id = it.id,
                    aggregationRef = RecordRef.valueOf("${AppName.EPROC}/proc-task@${it.id}"),
                    createTime = it.createTime
                )
            }

        val result = RecsQueryRes<RecordRef>()
        val aggregatedRecords = let {
            return@let (resultFromAlf.getRecords() + tasksFromCamunda)
                .sortedByDescending { it.createTime }
                .drop(originalSkip)
                .map { it.aggregationRef }
        }

        val aggregateTotalCount = resultFromAlf.getTotalCount() + camundaCount

        result.setRecords(aggregatedRecords.take(recsQuery.page.maxItems))
        result.setTotalCount(resultFromAlf.getTotalCount() + camundaCount)
        result.setHasMore(aggregateTotalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)

        return result
    }
}
