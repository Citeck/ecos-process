package ru.citeck.ecos.process.domain.proc.dto

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class NewProcessDefDto(
    val id: String,
    val name: MLText = MLText.EMPTY,
    val procType: String,
    val workspace: String,
    val format: String,
    val alfType: String? = null,
    val ecosTypeRef: EntityRef = EntityRef.EMPTY,
    val formRef: EntityRef = EntityRef.EMPTY,
    val workingCopySourceRef: EntityRef = EntityRef.EMPTY,
    val data: ByteArray,
    val image: ByteArray?,
    val enabled: Boolean = false,
    val autoStartEnabled: Boolean = false,
    val autoDeleteEnabled: Boolean = true,
    val sectionRef: EntityRef = EntityRef.EMPTY,
    val createdFromVersion: EntityRef = EntityRef.EMPTY,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewProcessDefDto) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (procType != other.procType) return false
        if (workspace != other.workspace) return false
        if (format != other.format) return false
        if (alfType != other.alfType) return false
        if (ecosTypeRef != other.ecosTypeRef) return false
        if (formRef != other.formRef) return false
        if (!data.contentEquals(other.data)) return false
        if (!image.contentEquals(other.image)) return false
        if (sectionRef != other.sectionRef) return false
        if (createdFromVersion != other.createdFromVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + procType.hashCode()
        result = 31 * result + workspace.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (alfType?.hashCode() ?: 0)
        result = 31 * result + ecosTypeRef.hashCode()
        result = 31 * result + formRef.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + image.contentHashCode()
        result = 31 * result + sectionRef.hashCode()
        result = 31 * result + createdFromVersion.hashCode()
        return result
    }

    override fun toString(): String {
        return "NewProcessDefDto(id='$id', name=$name, procType='$procType', workspace='$workspace' " +
            "format='$format', alfType=$alfType, ecosTypeRef=$ecosTypeRef, formRef=$formRef, enabled=$enabled, " +
            "autoStartEnabled=$autoStartEnabled, autoDeleteEnabled=$autoDeleteEnabled, " +
            "sectionRef=$sectionRef, createdFromVersion=$createdFromVersion)"
    }
}
