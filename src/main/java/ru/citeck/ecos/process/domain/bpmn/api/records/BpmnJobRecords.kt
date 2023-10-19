package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao

@Component
class BpmnJobRecords : AbstractRecordsDao() {

    companion object {
        const val ID = "bpmn-job"
    }

    override fun getId() = ID
}
