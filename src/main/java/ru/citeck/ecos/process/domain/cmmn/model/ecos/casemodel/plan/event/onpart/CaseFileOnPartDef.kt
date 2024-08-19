package ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import ru.citeck.ecos.commons.data.MLText

class CaseFileOnPartDef(
    val id: String,
    val name: MLText,
    val sourceRef: String,
    val standardEvent: CaseFileItemTransitionEnum
)

enum class CaseFileItemTransitionEnum(private val value: String) {

    ADD_CHILD("addChild"),
    ADD_REFERENCE("addReference"),
    CREATE("create"),
    DELETE("delete"),
    REMOVE_CHILD("removeChild"),
    REMOVE_REFERENCE("removeReference"),
    REPLACE("replace"),
    UPDATE("update");

    companion object {

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): CaseFileItemTransitionEnum {
            return values().firstOrNull { it.value == value } ?: error("Value is not found: '$value'")
        }
    }

    @JsonValue
    fun getValue(): String {
        return value
    }
}
