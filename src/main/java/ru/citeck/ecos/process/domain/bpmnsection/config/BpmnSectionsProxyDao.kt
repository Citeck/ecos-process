package ru.citeck.ecos.process.domain.bpmnsection.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy

@Component
class BpmnSectionsProxyDao : RecordsDaoProxy(
    id = SOURCE_ID,
    targetId = TARGET_SOURCE_ID
) {

    companion object {
        const val SOURCE_ID = "bpmn-section"
        const val TARGET_SOURCE_ID = BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID
    }

    override fun delete(recordsId: List<String>): List<DelStatus> {
        if (recordsId.contains("DEFAULT")) {
            error("You can't delete default section")
        }
        return super.delete(recordsId)
    }
}
