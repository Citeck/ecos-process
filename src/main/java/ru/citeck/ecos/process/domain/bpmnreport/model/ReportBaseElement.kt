package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

open class ReportBaseElement(
    var type: String = "",
    var name: MLText? = null,
    var documentation: MLText? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReportBaseElement

        if (type != other.type) return false
        if (name != other.name) return false
        if (documentation != other.documentation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (documentation?.hashCode() ?: 0)
        return result
    }
}
