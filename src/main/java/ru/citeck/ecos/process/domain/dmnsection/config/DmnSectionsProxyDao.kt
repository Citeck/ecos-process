package ru.citeck.ecos.process.domain.dmnsection.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.section.records.SectionRecordsUtils
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes

const val DMN_SECTIONS_RECORDS_ID = "dmn-section"

@Component
class DmnSectionsProxyDao : RecordsDaoProxy(
    id = DMN_SECTIONS_RECORDS_ID,
    targetId = DMN_SECTION_REPO_SOURCE_ID
) {

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        @Suppress("UNCHECKED_CAST")
        val queryRes = super.queryRecords(recsQuery) as? RecsQueryRes<Any> ?: return null
        queryRes.setRecords(SectionRecordsUtils.moveRootSectionToTop(queryRes.getRecords()))
        return queryRes
    }

    override fun delete(recordIds: List<String>): List<DelStatus> {
        if (recordIds.contains("DEFAULT") || recordIds.contains("ROOT")) {
            error("You can't delete default section")
        }
        return super.delete(recordIds)
    }
}
