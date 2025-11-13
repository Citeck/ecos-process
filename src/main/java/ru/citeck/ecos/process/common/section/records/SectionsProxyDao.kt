package ru.citeck.ecos.process.common.section.records

import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import ru.citeck.ecos.webapp.api.entity.toEntityRef

class SectionsProxyDao(
    id: String,
    targetId: String,
    private val sectionType: SectionType
) : RecordsDaoProxy(id, targetId) {

    companion object {
        const val SECTION_ROOT = "ROOT"
        const val SECTION_DEFAULT = "DEFAULT"

        val PROTECTED_SECTIONS = setOf(
            SECTION_DEFAULT,
            SECTION_ROOT
        )
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return super.mutate(records)
        }
        records.forEach {

            if (it.id.isBlank()) {

                val parentRef = it.getAtt("parentRef")
                    .asText()
                    .toEntityRef()
                    .ifEmpty { EntityRef.create(getId(), SECTION_ROOT) }

                val canCreateChildren = recordsService.getAtt(
                    parentRef,
                    "permissions._has.${sectionType.createSectionPermissionId}?bool!"
                ).asBoolean()

                if (!canCreateChildren) {
                    error("Permission denied")
                }
            } else {
                val hasPermissionToEdit = recordsService.getAtt(
                    sectionType.getRef(it.id),
                    "permissions._has.write?bool!"
                ).asBoolean()
                if (!hasPermissionToEdit) {
                    error("Permission denied")
                }
            }
        }
        return super.mutate(records)
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        @Suppress("UNCHECKED_CAST")
        val queryRes = super.queryRecords(recsQuery) as? RecsQueryRes<Any> ?: return null
        queryRes.setRecords(moveRootSectionToTop(queryRes.getRecords()))
        return queryRes
    }

    override fun delete(recordIds: List<String>): List<DelStatus> {
        if (recordIds.any { PROTECTED_SECTIONS.contains(it) }) {
            error("You can't delete protected section")
        }
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return super.delete(recordIds)
        }

        recordIds.forEach {
            val hasPermissionToEdit = recordsService.getAtt(
                sectionType.getRef(it),
                "permissions._has.write?bool!"
            ).asBoolean()
            if (!hasPermissionToEdit) {
                error("Permission denied")
            }
        }

        return super.delete(recordIds)
    }

    private fun <T : Any> moveRootSectionToTop(values: List<T>): List<T> {
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
