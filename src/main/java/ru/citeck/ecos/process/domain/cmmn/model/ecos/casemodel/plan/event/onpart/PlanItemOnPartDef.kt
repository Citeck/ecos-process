package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import ru.citeck.ecos.commons.data.MLText

class PlanItemOnPartDef(
    val id: String,
    val name: MLText,
    val sourceRef: String,
    val standardEvent: PlanItemTransitionEnum,
    val exitCriterionRef: String?
)

enum class PlanItemTransitionEnum(private val value: String) {

    CLOSE("close"),
    COMPLETE("complete"),
    CREATE("create"),
    DISABLE("disable"),
    ENABLE("enable"),
    EXIT("exit"),
    FAULT("fault"),
    MANUAL_START("manualStart"),
    OCCUR("occur"),
    PARENT_RESUME("parentResume"),
    PARENT_SUSPEND("parentSuspend"),
    REACTIVATE("reactivate"),
    REENABLE("reenable"),
    RESUME("resume"),
    START("start"),
    SUSPEND("suspend"),
    TERMINATE("terminate");

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): PlanItemTransitionEnum {
            return values().firstOrNull { it.value == value } ?: error("Value is not found: '$value'")
        }
    }

    @JsonValue
    fun getValue(): String {
        return value
    }
}
