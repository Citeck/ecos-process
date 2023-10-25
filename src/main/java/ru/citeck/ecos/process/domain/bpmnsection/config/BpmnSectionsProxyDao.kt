package ru.citeck.ecos.process.domain.bpmnsection.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy

@Component
class BpmnSectionsProxyDao : RecordsDaoProxy(
    id = SOURCE_ID,
    targetId = BPMN_SECTION_REPO_SOURCE_ID
) {

    companion object {
        private const val SOURCE_ID = "bpmn-section"
    }

    override fun delete(recordIds: List<String>): List<DelStatus> {
        if (recordIds.contains("DEFAULT")) {
            error("You can't delete default section")
        }
        return super.delete(recordIds)
    }
}
