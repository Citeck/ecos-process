package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.status.constants.StatusConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

@Component
class CamundaStatusSetter(
    private val recordsService: RecordsService
) {

    fun setStatus(documentRef: RecordRef, status: String) {
        val recordAtts = RecordAtts(documentRef)
        recordAtts.setAtt(StatusConstants.ATT_STATUS, status)

        recordsService.mutate(recordAtts)
    }
}
