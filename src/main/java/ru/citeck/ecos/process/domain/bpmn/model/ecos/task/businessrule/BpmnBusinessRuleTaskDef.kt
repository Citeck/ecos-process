package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.common.toDecisionKey
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.RefBinding
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDecisionLatestRecords
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDecisionRecords
import ru.citeck.ecos.webapp.api.entity.EntityRef

private val allowedSourceIds = listOf(DmnDecisionRecords.ID, DmnDecisionLatestRecords.ID)

data class BpmnBusinessRuleTaskDef(
    val id: String,
    val name: MLText,
    val number: Int?,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val decisionRef: EntityRef,
    val decisionRefKey: String = decisionRef.toDecisionKey(),
    val binding: RefBinding,

    val resultVariable: String? = null,
    val mapDecisionResult: MapDecisionResult? = null,

    val version: Int? = null,
    val versionTag: String? = null,

    val multiInstanceConfig: MultiInstanceConfig? = null
) {

    init {
        if (decisionRef.isNotEmpty()) {
            val sourceId = decisionRef.getSourceId()

            if (sourceId !in allowedSourceIds) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "Invalid decisionRef sourceId: $decisionRef. Allowed sourceIds: $allowedSourceIds"
                )
            }

            val decisionKey = decisionRef.toDecisionKey()
            if (decisionKey.isBlank()) {
                throw EcosBpmnElementDefinitionException(
                    id,
                    "Decision key can't be blank: $decisionRef"
                )
            }
        }

        when (binding) {
            RefBinding.VERSION -> {
                if (version == null) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Version is required for Version decision binding"
                    )
                }
            }

            RefBinding.VERSION_TAG -> {
                if (versionTag.isNullOrBlank()) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Version tag is required for Version Tag decision binding"
                    )
                }
            }

            else -> {
                // do nothing
            }
        }
    }
}

enum class MapDecisionResult(val value: String) {
    COLLECT_ENTRIES("collectEntries"),
    RESULT_LIST("resultList"),
    SINGLE_ENTRY("singleEntry"),
    SINGLE_RESULT("singleResult")
}
