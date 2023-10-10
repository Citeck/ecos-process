package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportLaneElement(
    var name: MLText = MLText(),
    var documentation: MLText = MLText()
)
