package ru.citeck.ecos.process.common.section.records

import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef

object SectionRecordsUtils {

    fun <T : Any> moveRootSectionToTop(values: List<T>): List<T> {
        for (idx in values.indices) {

            val recId = getLocalId(values[idx])
            if (recId == "ROOT") {
                val result = ArrayList<T>()
                result.add(values[idx])
                for (innerIdx in values.indices) {
                    if (innerIdx != idx) {
                        result.add(values[innerIdx])
                    }
                }
                return result
            }
        }
        return values
    }

    private fun getLocalId(value: Any): String {
        var idValue = value
        if (value is AttValue) {
            idValue = value.id ?: return ""
        }
        if (value is String) {
            idValue = EntityRef.valueOf(value)
        }
        if (idValue is EntityRef) {
            return idValue.getLocalId()
        }
        return ""
    }
}
