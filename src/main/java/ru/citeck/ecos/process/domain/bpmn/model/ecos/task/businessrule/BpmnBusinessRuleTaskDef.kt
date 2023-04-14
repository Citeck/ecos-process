package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class BpmnBusinessRuleTaskDef(
    val id: String,
    val name: MLText,
    val documentation: MLText,
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),

    val asyncConfig: AsyncConfig,
    val jobConfig: JobConfig,

    val decisionRef: EntityRef,
    val decisionRefKey: String = decisionRef.getLocalId().split(":")[0],
    val binding: DecisionRefBinding,

    val resultVariable: String? = null,
    val mapDecisionResult: MapDecisionResult? = null,

    val version: Int? = null,
    val versionTag: String? = null,

    val multiInstanceConfig: MultiInstanceConfig? = null
) {

    init {
        if (decisionRef.isNotEmpty()) {
            decisionRef.getLocalId().split(":").let {
                if (it.size != 3) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Invalid decisionRef id format: $decisionRef"
                    )
                }

                val decisionKey = it[0]
                if (decisionKey.isBlank()) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Invalid decisionRef id key: $decisionRef"
                    )
                }
            }
        }

        when (binding) {
            DecisionRefBinding.VERSION -> {
                if (version == null) {
                    throw EcosBpmnElementDefinitionException(
                        id,
                        "Version is required for Version decision binding"
                    )
                }
            }
            DecisionRefBinding.VERSION_TAG -> {
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

enum class DecisionRefBinding(val value: String) {
    DEPLOYMENT("deployment"),
    LATEST("latest"),
    VERSION("version"),
    VERSION_TAG("versionTag")
}

enum class MapDecisionResult(val value: String) {
    COLLECT_ENTRIES("collectEntries"),
    RESULT_LIST("resultList"),
    SINGLE_ENTRY("singleEntry"),
    SINGLE_RESULT("singleResult")
}
