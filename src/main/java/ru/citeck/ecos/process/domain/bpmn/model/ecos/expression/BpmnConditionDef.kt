package ru.citeck.ecos.process.domain.bpmn.model.ecos.expression

import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnDefinitionException
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.Validated

data class BpmnConditionDef(
    val type: ConditionType = ConditionType.NONE,
    val config: ConditionConfig = ConditionConfig()
) : Validated {

    override fun validate() {
        when (type) {
            ConditionType.EXPRESSION -> {
                if (config.expression.isBlank()) {
                    throw EcosBpmnDefinitionException(
                        "On expression condition, expression cannot be blank. $this"
                    )
                }
            }

            ConditionType.OUTCOME -> {
                if (config.outcome == Outcome.EMPTY) {
                    throw EcosBpmnDefinitionException(
                        "On outcome condition, outcome cannot be empty. $this"
                    )
                }
            }

            ConditionType.SCRIPT -> {
                if (config.fn.isBlank()) {
                    throw EcosBpmnDefinitionException(
                        "On script condition, script cannot be blank. $this"
                    )
                }
            }

            else -> {}
        }
    }
}
