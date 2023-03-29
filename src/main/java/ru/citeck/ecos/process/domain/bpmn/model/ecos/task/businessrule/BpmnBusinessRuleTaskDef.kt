package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule

import ru.citeck.ecos.commons.data.MLText
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
                require(it.size == 3) { "Invalid decisionRef id format: $decisionRef" }

                val decisionKey = it[0]
                require(decisionKey.isNotBlank()) { "Invalid decisionRef id key: $decisionRef" }
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
