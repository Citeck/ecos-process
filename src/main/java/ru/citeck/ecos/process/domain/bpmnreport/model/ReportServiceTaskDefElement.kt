package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportServiceTaskDefElement(
    var type: MLText = MLText(),
    var topic: String? = null,
    var expression: String? = null
)
