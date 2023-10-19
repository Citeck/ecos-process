package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportSequenceElement(
    var name: MLText? = null,
    var type: MLText? = null,
    var outcome: MLText? = null
)
