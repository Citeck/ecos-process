package ru.citeck.ecos.process.domain.bpmnreport.api

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmnreport.service.BpmnProcessReportService
import ru.citeck.ecos.records3.record.atts.value.AttValueCtx
import ru.citeck.ecos.records3.record.mixin.AttMixin
import javax.annotation.PostConstruct

@Component
class BpmnProcessReportMixin(
    val dao: BpmnProcessDefRecords,
    val bpmnProcessReportService: BpmnProcessReportService
) : AttMixin {

    @PostConstruct
    fun init() {
        dao.addAttributesMixin(this)
    }

    override fun getAtt(path: String, value: AttValueCtx): Any? {
        if (path == "bpmn-report") {
            val definition = value.getAtt("definition?str").asText()
            val def = BpmnIO.importEcosBpmn(definition)

            return bpmnProcessReportService.generateReportElementListForBpmnDefinition(def)
        }

        return null
    }

    override fun getProvidedAtts(): Collection<String> {
        return listOf("bpmn-report")
    }

}
