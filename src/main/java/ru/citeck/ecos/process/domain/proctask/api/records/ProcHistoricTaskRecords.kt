package ru.citeck.ecos.process.domain.proctask.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.converter.TaskConverter
import ru.citeck.ecos.process.domain.proctask.service.HistoricTaskQueryBuilder
import ru.citeck.ecos.process.domain.proctask.service.ProcHistoricTaskService
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordsAttsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.system.measureTimeMillis

/**
 * @author Roman Makarskiy
 */
@Component
class ProcHistoricTaskRecords(
    private val configuration: ProcessEngineConfigurationImpl,
    private val syncService: ProcTaskAttsSyncService,
    private val authoritiesApi: EcosAuthoritiesApi,
    private val procHistoricTaskService: ProcHistoricTaskService,
    private val taskConverter: TaskConverter,
    private val procTaskRecords: ProcTaskRecords
) : AbstractRecordsDao(),
    RecordsQueryDao,
    RecordsAttsDao {

    companion object {
        const val ID = "proc-historic-task"

        private val log = KotlinLogging.logger {}
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val predicate = when (recsQuery.language) {
            PredicateService.LANGUAGE_PREDICATE -> recsQuery.getQuery(Predicate::class.java)
            "" -> {
                require(recsQuery.query.isNull() || recsQuery.query.isObject()) { "Unsupported historic task query" }
                if (recsQuery.query.has("t")) {
                    recsQuery.getQuery(Predicate::class.java)
                } else {
                    require(recsQuery.query.isEmpty()) { "Historic task query must be a predicate" }
                    Predicates.alwaysTrue()
                }
            }
            else -> error("Unsupported language: ${recsQuery.language}")
        }
        val page = recsQuery.page
        val tasks = HistoricTaskQueryBuilder(configuration, syncService, authoritiesApi)
            .select(predicate, recsQuery.sortBy, page.skipCount, page.maxItems)
        return RecsQueryRes<EntityRef>().apply {
            setRecords(tasks.entities.map { EntityRef.create(AppName.EPROC, ID, it) })
            setTotalCount(tasks.totalCount)
            setHasMore(page.skipCount.toLong() + tasks.entities.size < tasks.totalCount)
        }
    }

    override fun getRecordsAtts(recordIds: List<String>): List<ProcTaskRecords.ProcTaskRecord?> {
        if (recordIds.isEmpty()) {
            return emptyList()
        }

        val result: List<ProcTaskRecords.ProcTaskRecord?>
        val time = measureTimeMillis {
            result = procHistoricTaskService.getHistoricTasksByIds(recordIds).map {
                it?.let { historicTask -> taskConverter.toRecord(historicTask, procTaskRecords) }
            }
        }

        log.debug { "Get Camunda Historic Tasks records atts: $time ms" }

        return result
    }
}
