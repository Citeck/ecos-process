package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

open class ReportBaseElement(
    var type: String = "",
    var name: MLText = MLText(),
    var documentation: MLText = MLText()
)
