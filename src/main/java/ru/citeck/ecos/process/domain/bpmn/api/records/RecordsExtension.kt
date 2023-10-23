package ru.citeck.ecos.process.domain.bpmn.api.records

import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import java.util.*

interface IdentifiableRecord {

    fun getIdentificator(): String
}

fun <T : IdentifiableRecord> List<T>.sortByIds(ids: List<String>): List<T> {
    val map = associateBy { it.getIdentificator() }
    return ids.mapNotNull { map[it] }
}

fun <T : Enum<T>> LocalRecordAtts.toActionEnum(type: Class<T>): T? {
    this.attributes["action"].let {
        return if (it.isNotEmpty()) {
            type.enumConstants.find { enum -> enum.name == it.asText().uppercase(Locale.getDefault()) }
                ?: error("Unknown action: $it")
        } else {
            null
        }
    }
}

fun <T : Enum<T>> LocalRecordAtts.toActionEnumOrDefault(type: Class<T>, default: T): T {
    return toActionEnum(type) ?: default
}
