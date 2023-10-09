package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao

// TODO: implement
@Component
class BpmnIncidentRecords : AbstractRecordsDao() {

    companion object {
        const val ID = "bpmn-incident"
    }

    override fun getId() = ID
}
