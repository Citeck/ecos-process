package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportStatusElement(
    var name: MLText? = null,
    var status: MLText? = null
)
