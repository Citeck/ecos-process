package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportLaneElement(
    var name: MLText? = null,
    val number: Int? = null,
    var documentation: MLText? = null
)
