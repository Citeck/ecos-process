package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.status.constants.StatusConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class CamundaStatusSetter(
    private val recordsService: RecordsService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun setStatus(documentRef: EntityRef, status: String) {
        log.debug { "Set status $status for document $documentRef" }

        val recordAtts = RecordAtts(documentRef)
        recordAtts.setAtt(StatusConstants.ATT_STATUS, status)

        recordsService.mutate(recordAtts)
    }
}
