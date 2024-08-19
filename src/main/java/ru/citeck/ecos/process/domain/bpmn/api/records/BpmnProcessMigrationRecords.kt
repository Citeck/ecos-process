package ru.citeck.ecos.process.domain.bpmn.api.records

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.rest.MigrationRestService
import org.camunda.bpm.engine.rest.dto.batch.BatchDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationExecutionDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanDto
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanGenerationDto
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.service.BpmnPermissionResolver
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessMigrationRecords(
    private val camundaMigrationRestService: MigrationRestService,
    private val bpmnPermissionResolver: BpmnPermissionResolver
) :
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
        ).migrationPlanGeneration ?: error("Migration plan generation is required")

        check(
            migrationPlanGeneration.sourceProcessDefinitionId.isNotBlank() &&
                migrationPlanGeneration.targetProcessDefinitionId.isNotBlank()
        ) {
            "Source and target process definition ids are required"
        }

        if (!bpmnPermissionResolver.isAllowForBpmnDefEngine(
                BpmnPermission.PROC_INSTANCE_MIGRATE,
                BpmnProcessDefEngineRecords.createRef(migrationPlanGeneration.sourceProcessDefinitionId)
            ) || !bpmnPermissionResolver.isAllowForBpmnDefEngine(
                BpmnPermission.PROC_INSTANCE_MIGRATE,
                BpmnProcessDefEngineRecords.createRef(migrationPlanGeneration.targetProcessDefinitionId)
            )
        ) {
            return emptyList<EntityRef>()
        }

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

        val sourceDefEngine = migrationExecution.migrationPlan.sourceProcessDefinitionId
        val targetDefEngine = migrationExecution.migrationPlan.targetProcessDefinitionId
        check(
            bpmnPermissionResolver.isAllowForBpmnDefEngine(
                BpmnPermission.PROC_INSTANCE_MIGRATE,
                BpmnProcessDefEngineRecords.createRef(sourceDefEngine)
            ) &&
                bpmnPermissionResolver.isAllowForBpmnDefEngine(
                    BpmnPermission.PROC_INSTANCE_MIGRATE,
                    BpmnProcessDefEngineRecords.createRef(targetDefEngine)
                )
        ) {
            "User has no permissions to migrate process instance: $sourceDefEngine to $targetDefEngine"
        }

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

    enum class MutateAction {
        MIGRATE
    }

    data class ProcessMigrationRecord(
        var migrationPlan: MigrationPlanDto? = null
    )

    data class BpmnProcessMigrationQuery(
        var migrationPlanGeneration: MigrationPlanGenerationDto? = null
    )
}
