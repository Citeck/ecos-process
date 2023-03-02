package ru.citeck.ecos.process.domain.dmnsection.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy

const val DMN_SECTIONS_RECORDS_ID = "dmn-section"

@Component
class DmnSectionsProxyDao : RecordsDaoProxy(
    id = DMN_SECTIONS_RECORDS_ID,
    targetId = DMN_SECTION_REPO_SOURCE_ID
) {

    override fun delete(recordIds: List<String>): List<DelStatus> {
        if (recordIds.contains("DEFAULT")) {
            error("You can't delete default section")
        }
        return super.delete(recordIds)
    }
}
