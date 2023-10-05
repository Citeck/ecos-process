package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportEventElement(
    var eventType: MLText? = null,
    var value: String? = null,
) : ReportBaseElement()
