package ru.citeck.ecos.process.domain.proc.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.records2.RecordRef

data class NewProcessDefDto(
    val id: String,
    val name: MLText = MLText.EMPTY,
    val procType: String,
    val format: String,
    val alfType: String? = null,
    val ecosTypeRef: RecordRef = RecordRef.EMPTY,
    val formRef: RecordRef = RecordRef.EMPTY,
    val data: ByteArray,
    val enabled: Boolean = false,
    val autoStartEnabled: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewProcessDefDto) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (procType != other.procType) return false
        if (format != other.format) return false
        if (alfType != other.alfType) return false
        if (ecosTypeRef != other.ecosTypeRef) return false
        if (formRef != other.formRef) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + procType.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (alfType?.hashCode() ?: 0)
        result = 31 * result + ecosTypeRef.hashCode()
        result = 31 * result + formRef.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "NewProcessDefDto(id='$id', name=$name, procType='$procType', format='$format', " +
            "alfType=$alfType, ecosTypeRef=$ecosTypeRef, formRef=$formRef, enabled=$enabled, " +
            "autoStartEnabled=$autoStartEnabled)"
    }
}
