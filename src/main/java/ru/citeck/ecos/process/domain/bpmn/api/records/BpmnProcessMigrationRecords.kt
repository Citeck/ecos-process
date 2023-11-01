package ru.citeck.ecos.process.domain.bpmn.api.records

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.camunda.bpm.engine.rest.MigrationRestService
import org.camunda.bpm.engine.rest.dto.batch.BatchDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationExecutionDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanGenerationDto
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

// TODO: permissions and tests
@Component
class BpmnProcessMigrationRecords(private val camundaMigrationRestService: MigrationRestService) :
    AbstractRecordsDao(),
    RecordsQueryDao,
    RecordMutateDao {

    companion object {
        const val ID = "bpmn-process-migration"

        private val log = KotlinLogging.logger {}

        // We can't use our Json.mapper for convert ProcessInstanceModificationDto
        private val standardMapper = ObjectMapper()

        private const val ATT_MIGRATION_EXECUTION = "migrationExecution"
        private const val ATT_ASYNC = "async"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): Any? {
        val migrationPlanGeneration = recsQuery.getQuery(
            BpmnProcessMigrationQuery::class.java
        ).migrationPlanGeneration ?: return null

        val plan = camundaMigrationRestService.generateMigrationPlan(migrationPlanGeneration)

        return ProcessMigrationRecord(plan)
    }

    override fun mutate(record: LocalRecordAtts): String {
        return when (record.toActionEnum(MutateAction::class.java)) {
            MutateAction.MIGRATE -> migrateProcess(record).toString()

            else -> {
                error("Unknown action: $record")
            }
        }
    }

    private fun migrateProcess(record: LocalRecordAtts): EntityRef {
        log.debug { "Migrate process: \n${Json.mapper.toPrettyString(record)}" }

        val migrationData = record.getAtt(ATT_MIGRATION_EXECUTION).toString()
        if (migrationData.isBlank()) {
            error("Migration data is not specified")
        }

        val migrationExecution: MigrationExecutionDto = standardMapper.readValue(
            migrationData,
            MigrationExecutionDto::class.java
        )
        val isAsync = record.getAtt(ATT_ASYNC).asBoolean()

        val batchResult = if (isAsync) {
            val batch: BatchDto = camundaMigrationRestService.executeMigrationPlanAsync(migrationExecution)
            EntityRef.create(AppName.EPROC, BpmnHistoricBatchRecords.ID, batch.id)
        } else {
            camundaMigrationRestService.executeMigrationPlan(migrationExecution)
            EntityRef.EMPTY
        }

        log.debug { "Migration batch result: $batchResult" }

        return batchResult
    }

    private enum class MutateAction {
        MIGRATE;
    }

    private inner class ProcessMigrationRecord(
        val migrationPlan: MigrationPlanDto? = null
    )

    data class BpmnProcessMigrationQuery(
        var migrationPlanGeneration: MigrationPlanGenerationDto? = null
    )
}
