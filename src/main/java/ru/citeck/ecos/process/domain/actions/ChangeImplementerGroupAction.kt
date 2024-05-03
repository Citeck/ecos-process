package ru.citeck.ecos.process.domain.actions

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.groupactions.context.GroupActionContext
import ru.citeck.ecos.groupactions.execution.GroupActionExecution
import ru.citeck.ecos.groupactions.execution.GroupActionExecutionFactory
import ru.citeck.ecos.groupactions.execution.result.ActionResult
import ru.citeck.ecos.groupactions.execution.result.ActionResultResults
import ru.citeck.ecos.groupactions.execution.result.ActionResultResults.Result
import ru.citeck.ecos.groupactions.execution.result.ActionResultResults.ResultStatus
import ru.citeck.ecos.process.domain.proctask.api.records.CHANGE_OWNER_RECORD_ACTION_CLAIM
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class ChangeImplementerGroupAction(
    recordServices: RecordsServiceFactory,
    private val recordService: RecordsService,
) : GroupActionExecutionFactory<RecordAtts, ChangeImplementerGroupAction.Config> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val schemaReader = recordServices.dtoSchemaReader
    private val schemaWriter = recordServices.attSchemaWriter

    override fun getType(): String {
        return "change-owner"
    }

    override fun createExecution(config: Config): GroupActionExecution<RecordAtts> {
        return Execution(config)
    }

    private inner class Execution(val config: Config) : GroupActionExecution<RecordAtts> {

        override fun getRequiredAttributes(): Map<String, *> {
            return schemaWriter.writeToMap(schemaReader.read(ValueAtts::class.java))
        }

        override fun execute(context: GroupActionContext<RecordAtts>): ActionResult {
            val results = ArrayList<Result>()
            for (record in context.getValues()) {
                val recordId = record.getId()
                try {
                    val taskId = recordService.queryOne(
                        RecordsQuery.create()
                            .withSourceId("eproc/wftask")
                            .withQuery(mapOf("active" to true, "document" to "$recordId"))
                            .build()
                    ) ?: EntityRef.EMPTY

                    recordService.mutate(
                        taskId,
                        ChangeOwnerMutationAtts(
                            mapOf(
                                "action" to CHANGE_OWNER_RECORD_ACTION_CLAIM,
                                "owner" to config.owner
                            )
                        )
                    )
                    results.add(Result("", ResultStatus.OK, recordId))
                } catch (e: Exception) {
                    results.add(Result("${e.message}", ResultStatus.ERROR, recordId))
                    log.error(e) { "Failed to execute the action. Error message: ${e.message}" }
                }
            }
            return ActionResultResults(results)
        }

        override fun dispose() {
        }
    }

    class Config(
        val owner: String
    )

    class ValueAtts(
        @AttName(ScalarType.ID_SCHEMA)
        val id: EntityRef
    )

    data class ChangeOwnerMutationAtts(
        val changeOwner: Map<String, String>
    )
}
